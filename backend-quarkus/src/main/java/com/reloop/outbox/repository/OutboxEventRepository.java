package com.reloop.outbox.repository;

import com.reloop.common.jpa.ReloopRepository;
import com.reloop.outbox.domain.OutboxEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OutboxEventRepository implements ReloopRepository<OutboxEvent, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Claims a single event with FOR UPDATE SKIP LOCKED so concurrent workers
     * never process the same event twice, and each event is handled in its own
     * short transaction (no long-held locks while dispatching).
     */
    @SuppressWarnings("unchecked")
    public Optional<OutboxEvent> claimNextPendingEvent(Instant now) {
        List<OutboxEvent> events = entityManager
                .createNativeQuery("""
                        SELECT * FROM outbox_events
                        WHERE status IN ('PENDING', 'FAILED')
                          AND (next_retry_at IS NULL OR next_retry_at <= :now)
                        ORDER BY created_at ASC
                        FOR UPDATE SKIP LOCKED
                        LIMIT 1
                        """, OutboxEvent.class)
                .setParameter("now", now)
                .getResultList();
        return events.stream().findFirst();
    }
}
