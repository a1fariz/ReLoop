package com.reloop.outbox.service;

import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OutboxPollerWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxPollerWorker.class);
    private final OutboxEventRepository outboxEventRepository;

    public OutboxPollerWorker(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEventsForProcessing(Instant.now());
        if (events.isEmpty()) return;

        for (OutboxEvent event : events) {
            try {
                log.info("Processing outbox event: [type={}] [id={}] [corr_id={}]",
                        event.getEventType(), event.getId(), event.getCorrelationId());

                // Simulated async side effect dispatch (e.g. Email/Push/WebSocket/Webhook)
                event.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception ex) {
                log.error("Failed to process outbox event {}: {}", event.getId(), ex.getMessage());
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);

                if (retries >= event.getMaxRetries()) {
                    event.setStatus(OutboxEvent.OutboxStatus.DEAD_LETTER);
                } else {
                    event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                    // Exponential backoff: 2s, 4s, 8s, 16s...
                    long delaySeconds = (long) Math.pow(2, retries);
                    event.setNextRetryAt(Instant.now().plus(delaySeconds, ChronoUnit.SECONDS));
                }
                event.setLastError(ex.getMessage());
            }
            outboxEventRepository.save(event);
        }
    }
}
