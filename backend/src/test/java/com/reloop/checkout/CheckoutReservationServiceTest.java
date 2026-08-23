package com.reloop.checkout;

import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.dto.ReserveUnitRequest;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.checkout.service.CheckoutReservationService;
import com.reloop.common.exception.BusinessException;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutReservationServiceTest {

    @Mock
    private ProductUnitRepository productUnitRepository;

    @Mock
    private UnitReservationRepository reservationRepository;

    @InjectMocks
    private CheckoutReservationService reservationService;

    @Test
    @DisplayName("Should successfully acquire 15-minute lease for AVAILABLE unit")
    void testSuccessfulReservation() {
        UUID unitId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN123456", 100L, ProductUnit.UnitStatus.AVAILABLE, "A+");

        when(productUnitRepository.findByIdForUpdate(unitId)).thenReturn(Optional.of(unit));
        when(reservationRepository.findByUnitIdAndStatus(unit.getId(), UnitReservation.ReservationStatus.ACTIVE)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(UnitReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = reservationService.createReservationLease(1L, new ReserveUnitRequest(unitId, listingId));

        assertThat(response).isNotNull();
        assertThat(response.unitId()).isEqualTo(unit.getId());
        assertThat(response.remainingSeconds()).isGreaterThan(0);
        assertThat(unit.getStatus()).isEqualTo(ProductUnit.UnitStatus.RESERVED);
    }

    @Test
    @DisplayName("Should reject reservation if unit is already RESERVED or SOLD")
    void testRejectNonAvailableUnit() {
        UUID unitId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN123456", 100L, ProductUnit.UnitStatus.SOLD, "A+");

        when(productUnitRepository.findByIdForUpdate(unitId)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> reservationService.createReservationLease(1L, new ReserveUnitRequest(unitId, listingId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available for reservation");
    }
}
