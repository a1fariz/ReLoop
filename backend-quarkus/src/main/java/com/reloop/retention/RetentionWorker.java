package com.reloop.retention;

import com.reloop.auth.repository.RefreshTokenRepository;
import com.reloop.checkout.repository.IdempotencyKeyRecordRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Nightly cleanup so append-only tables don't grow without bound:
 * expired/revoked refresh tokens, consumed idempotency keys, processed outbox
 * events (7 days) and dead-lettered events (30 days).
 */
@ApplicationScoped
public class RetentionWorker {
    private static final Logger log = Logger.getLogger(RetentionWorker.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final IdempotencyKeyRecordRepository idempotencyKeyRecordRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Inject
    public RetentionWorker(
            RefreshTokenRepository refreshTokenRepository,
            IdempotencyKeyRecordRepository idempotencyKeyRecordRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.idempotencyKeyRecordRepository = idempotencyKeyRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(cron = "{reloop.retention.cron:0 0 3 * * ?}")
    @Transactional
    public void purge() {
        Instant now = Instant.now();

        long tokens = refreshTokenRepository.delete("expiresAt < ?1 OR isRevoked = true", now);
        long idempotencyKeys = idempotencyKeyRecordRepository.delete("expiresAt < ?1", now);
        long processedEvents = outboxEventRepository.delete(
                "status = ?1 AND processedAt < ?2", OutboxEvent.OutboxStatus.PROCESSED, now.minus(7, ChronoUnit.DAYS));
        long deadLetters = outboxEventRepository.delete(
                "status = ?1 AND createdAt < ?2", OutboxEvent.OutboxStatus.DEAD_LETTER, now.minus(30, ChronoUnit.DAYS));

        if (tokens + idempotencyKeys + processedEvents + deadLetters > 0) {
            log.infof("Retention purge: %d refresh tokens, %d idempotency keys, %d processed events, %d dead letters",
                    tokens, idempotencyKeys, processedEvents, deadLetters);
        }
    }
}
