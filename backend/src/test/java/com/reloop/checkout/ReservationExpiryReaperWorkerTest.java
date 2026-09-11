package com.reloop.checkout;

import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.checkout.service.ReservationExpiryReaperWorker;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpiryReaperWorkerTest {

    @Mock
    private UnitReservationRepository reservationRepository;

    @Mock
    private ProductUnitRepository productUnitRepository;

    @InjectMocks
    private ReservationExpiryReaperWorker reaperWorker;

    @Test
    @DisplayName("Should expire reservations and restore RESERVED unit state back to AVAILABLE")
    void testReapExpiredReservations() {
        UUID unitId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UnitReservation expiredReservation = new UnitReservation(unitId, 1L, listingId, Instant.now().minus(5, ChronoUnit.MINUTES));
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN123456", 2L, ProductUnit.UnitStatus.RESERVED, "A+");

        when(reservationRepository.findExpiredActiveReservations(any(Instant.class)))
                .thenReturn(List.of(expiredReservation));
        when(productUnitRepository.findByIdForUpdate(unitId)).thenReturn(Optional.of(unit));

        reaperWorker.reapExpiredReservations();

        assertThat(expiredReservation.getStatus()).isEqualTo(UnitReservation.ReservationStatus.EXPIRED);
        assertThat(unit.getStatus()).isEqualTo(ProductUnit.UnitStatus.AVAILABLE);
        verify(reservationRepository).save(expiredReservation);
        verify(productUnitRepository).save(unit);
    }
}
