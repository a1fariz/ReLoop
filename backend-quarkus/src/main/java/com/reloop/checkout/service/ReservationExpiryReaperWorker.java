package com.reloop.checkout.service;

import com.reloop.checkout.domain.UnitReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ReservationExpiryReaperWorker {
    private static final Logger log = Logger.getLogger(ReservationExpiryReaperWorker.class);

    private final UnitReservationRepository reservationRepository;
    private final ProductUnitRepository productUnitRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final java.util.UUID instanceCorrelationId = java.util.UUID.randomUUID();

    @Inject
    public ReservationExpiryReaperWorker(
            UnitReservationRepository reservationRepository,
            ProductUnitRepository productUnitRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            io.micrometer.core.instrument.MeterRegistry meterRegistry
    ) {
        this.reservationRepository = reservationRepository;
        this.productUnitRepository = productUnitRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(every = "5s")
    @Transactional
    public void reapExpiredReservations() {
        List<UnitReservation> expiredReservations = reservationRepository.findExpiredActiveReservations(Instant.now());
        if (expiredReservations.isEmpty()) {
            return;
        }

        for (UnitReservation reservation : expiredReservations) {
            try {
                reservation.setStatus(UnitReservation.ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);

                productUnitRepository.findByIdForUpdate(reservation.getUnitId()).ifPresent(unit -> {
                    if (unit.getStatus() == ProductUnit.UnitStatus.RESERVED) {
                        unit.setStatus(ProductUnit.UnitStatus.AVAILABLE);
                        productUnitRepository.save(unit);
                        emitLeaseExpired(reservation);
                        meterRegistry.counter("reloop.checkout.lease.expired").increment();
                        log.infof("Reaped expired reservation %s for unit %s. Unit state restored to AVAILABLE.",
                                reservation.getId(), unit.getId());
                    }
                });
            } catch (Exception ex) {
                log.errorf(ex, "Failed reaping reservation %s", reservation.getId());
            }
        }
    }

    private void emitLeaseExpired(UnitReservation reservation) {
        try {
            String payload = objectMapper.writeValueAsString(new LeaseExpiredPayload(
                    reservation.getId(), reservation.getUnitId(), reservation.getUserId()));
            outboxEventRepository.save(new OutboxEvent(
                    "RESERVATION",
                    reservation.getId().toString(),
                    "LEASE_EXPIRED",
                    payload,
                    instanceCorrelationId,
                    "LEASE_EXPIRED:" + reservation.getId()
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize LEASE_EXPIRED payload for reservation %s", reservation.getId());
        }
    }

    record LeaseExpiredPayload(
            java.util.UUID reservationId,
            java.util.UUID unitId,
            Long userId
    ) {}
}
