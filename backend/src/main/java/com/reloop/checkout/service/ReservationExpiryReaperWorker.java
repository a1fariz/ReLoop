package com.reloop.checkout.service;

import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ReservationExpiryReaperWorker {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryReaperWorker.class);

    private final UnitReservationRepository reservationRepository;
    private final ProductUnitRepository productUnitRepository;

    public ReservationExpiryReaperWorker(
            UnitReservationRepository reservationRepository,
            ProductUnitRepository productUnitRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.productUnitRepository = productUnitRepository;
    }

    @Scheduled(fixedDelay = 5000)
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
                        log.info("Reaped expired reservation {} for unit {}. Unit state restored to AVAILABLE.",
                                reservation.getId(), unit.getId());
                    }
                });
            } catch (Exception ex) {
                log.error("Failed reaping reservation {}", reservation.getId(), ex);
            }
        }
    }
}
