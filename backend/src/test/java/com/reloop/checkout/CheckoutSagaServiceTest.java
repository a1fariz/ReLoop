package com.reloop.checkout;

import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.dto.ConfirmPaymentRequest;
import com.reloop.checkout.dto.OrderConfirmationResponse;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.checkout.service.CheckoutSagaService;
import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.repository.ListingRepository;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutSagaServiceTest {

    @Mock
    private UnitReservationRepository reservationRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private ProductUnitRepository productUnitRepository;
    @Mock
    private MasterOrderRepository masterOrderRepository;
    @Mock
    private FulfillmentOrderRepository fulfillmentOrderRepository;
    @Mock
    private DoubleEntryLedgerService ledgerService;

    @InjectMocks
    private CheckoutSagaService checkoutSagaService;

    @Test
    @DisplayName("Successfully execute Checkout Saga with double-entry escrow hold and unit status change to SOLD")
    void testSuccessfulCheckoutSaga() {
        UUID token = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        UnitReservation reservation = new UnitReservation(unitId, 1L, listingId, Instant.now().plus(10, ChronoUnit.MINUTES));
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN9999", 2L, ProductUnit.UnitStatus.RESERVED, "A+");
        Listing listing = new Listing(unitId, 2L, "iPhone 15 Pro", "desc", new BigDecimal("10000000.00"), "A+");

        when(reservationRepository.findByToken(token)).thenReturn(Optional.of(reservation));
        when(productUnitRepository.findByIdForUpdate(unitId)).thenReturn(Optional.of(unit));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(masterOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fulfillmentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
                token,
                "SIMULATED_ESCROW_DIRECT",
                "Jl. Sudirman No 12, Jakarta"
        );

        OrderConfirmationResponse response = checkoutSagaService.processPaymentAndSettleOrder(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("10000000.00"));
        assertThat(response.platformFeeAmount()).isEqualByComparingTo(new BigDecimal("1500000.00")); // 15%
        assertThat(response.sellerNetAmount()).isEqualByComparingTo(new BigDecimal("8500000.00"));  // 85%
        assertThat(unit.getStatus()).isEqualTo(ProductUnit.UnitStatus.SOLD);
        assertThat(listing.getStatus()).isEqualTo(Listing.ListingStatus.SOLD);
        assertThat(reservation.getStatus()).isEqualTo(UnitReservation.ReservationStatus.CONVERTED);

        verify(ledgerService).postJournal(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Reject checkout saga when 15-min reservation lease is expired")
    void testRejectExpiredReservationLease() {
        UUID token = UUID.randomUUID();
        UnitReservation expiredReservation = new UnitReservation(UUID.randomUUID(), 1L, UUID.randomUUID(), Instant.now().minus(2, ChronoUnit.MINUTES));

        when(reservationRepository.findByToken(token)).thenReturn(Optional.of(expiredReservation));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(token, "GATEWAY", "Address");

        assertThatThrownBy(() -> checkoutSagaService.processPaymentAndSettleOrder(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reservation lease has expired");
    }
}
