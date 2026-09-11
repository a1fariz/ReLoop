package com.reloop.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Claims and dispatches exactly one outbox event per transaction: a slow broker
 * can never inflate lock retention across a whole batch, and one poison event
 * cannot roll back others.
 */
@ApplicationScoped
public class OutboxEventProcessor {
    private static final Logger log = Logger.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Emitter<String> kafkaEmitter;
    private final Mailer mailer;
    private final String dispatchMode;
    private final boolean mailEnabled;

    @Inject
    public OutboxEventProcessor(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Channel("reloop-events") Emitter<String> kafkaEmitter,
            Mailer mailer,
            @ConfigProperty(name = "reloop.outbox.dispatch", defaultValue = "LOG") String dispatchMode,
            @ConfigProperty(name = "reloop.mail.enabled", defaultValue = "true") boolean mailEnabled
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.kafkaEmitter = kafkaEmitter;
        this.mailer = mailer;
        this.dispatchMode = dispatchMode;
        this.mailEnabled = mailEnabled;
    }

    @Transactional
    public boolean claimAndProcessOne() {
        OutboxEvent event = outboxEventRepository.claimNextPendingEvent(Instant.now()).orElse(null);
        if (event == null) {
            return false;
        }

        try {
            dispatch(event);

            event.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            event.setLastError(null);
        } catch (Exception ex) {
            log.errorf(ex, "Failed to dispatch outbox event %s", event.getId());
            int retries = event.getRetryCount() + 1;
            event.setRetryCount(retries);

            if (retries >= event.getMaxRetries()) {
                event.setStatus(OutboxEvent.OutboxStatus.DEAD_LETTER);
            } else {
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                // Exponential backoff: 2s, 4s, 8s, 16s...
                long delaySeconds = (long) Math.pow(2, retries);
                event.setNextRetryAt(Instant.now().plus(delaySeconds, java.time.temporal.ChronoUnit.SECONDS));
            }
            event.setLastError(ex.getMessage());
        }

        outboxEventRepository.save(event);
        return true;
    }

    private void dispatch(OutboxEvent event) throws Exception {
        if ("EMAIL".equalsIgnoreCase(event.getAggregateType()) && mailEnabled) {
            sendEmail(event);
            return;
        }

        switch (dispatchMode.toUpperCase()) {
            case "KAFKA" -> publishToKafka(event);
            case "LOG" -> log.infof("Processing outbox event: [type=%s] [id=%s] [corr_id=%s]",
                    event.getEventType(), event.getId(), event.getCorrelationId());
            default -> throw new IllegalStateException("Unknown outbox dispatch mode: " + dispatchMode);
        }
    }

    private void publishToKafka(OutboxEvent event) throws Exception {
        String envelope = objectMapper.writeValueAsString(new EventEnvelope(
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getCorrelationId(),
                event.getIdempotencyKey(),
                event.getPayload()
        ));

        // Blocking wait so a Kafka outage surfaces here and drives the retry/backoff path
        kafkaEmitter.send(envelope).toCompletableFuture().get(5, TimeUnit.SECONDS);

        log.infof("Published outbox event to Kafka: [type=%s] [id=%s] [corr_id=%s]",
                event.getEventType(), event.getId(), event.getCorrelationId());
    }

    private void sendEmail(OutboxEvent event) {
        try {
            com.fasterxml.jackson.databind.JsonNode payload = objectMapper.readTree(event.getPayload());
            String to = payload.path("to").asText(null);
            String subject = payload.path("subject").asText("ReLoop notification");
            String body = payload.path("body").asText("");

            if (to == null || to.isBlank()) {
                log.warnf("Outbox email event %s has no recipient; skipping", event.getId());
                return;
            }

            mailer.send(Mail.withText(to, subject, body));

            log.infof("Sent outbox email: [event=%s] [to=%s] [corr_id=%s]",
                    event.getEventType(), to, event.getCorrelationId());
        } catch (Exception e) {
            throw new RuntimeException("Email dispatch failed for event " + event.getId(), e);
        }
    }

    public record EventEnvelope(
            String eventType,
            String aggregateType,
            String aggregateId,
            UUID correlationId,
            String idempotencyKey,
            String payload
    ) {}
}
