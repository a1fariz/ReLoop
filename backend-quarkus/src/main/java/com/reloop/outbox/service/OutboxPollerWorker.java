package com.reloop.outbox.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Scheduler only: delegates each event to {@link OutboxEventProcessor} so every
 * event is claimed, dispatched, and finalized in its own short transaction.
 */
@ApplicationScoped
public class OutboxPollerWorker {
    private static final Logger log = Logger.getLogger(OutboxPollerWorker.class);
    private static final int MAX_EVENTS_PER_TICK = 50;

    private final OutboxEventProcessor processor;

    @Inject
    public OutboxPollerWorker(OutboxEventProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(every = "2s")
    public void processOutboxEvents() {
        for (int i = 0; i < MAX_EVENTS_PER_TICK; i++) {
            if (!processor.claimAndProcessOne()) {
                return;
            }
        }
        log.warnf("Outbox backlog exceeds %d events; next tick will continue", MAX_EVENTS_PER_TICK);
    }
}
