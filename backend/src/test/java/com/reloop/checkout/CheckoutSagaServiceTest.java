package com.reloop.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.checkout.domain.IdempotencyKeyRecord;
import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.dto.ConfirmPaymentRequest;
import com.reloop.checkout.dto.OrderConfirmationResponse;
import com.reloop.checkout.repository.IdempotencyKeyRecordRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock
    private IdempotencyKeyRecordRepository idempotencyKeyRecordRepository;

    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private CheckoutSagaService checkoutSagaService;

    @BeforeEach
    void setUp() {
        checkoutSagaService = new CheckoutSagaService(
                reservationRepository,
                listingRepository,
                productUnitRepository,
                masterOrderRepository,
                fulfillmentOrderRepository,
                ledgerService,
                idempotencyKeyRecordRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Successfully execute Checkout Saga with double-entry escrow hold and unit status change to SOLD")
    void testSuccessfulCheckoutSaga() {
        UUID token = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        UnitReservation reservation = new UnitReservation(unitId, 1L, listingId, Instant.now().plus(10, ChronoUnit.MINUTES));
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN9999", 2L, ProductUnit.UnitStatus.RESERVED, "A+");
        org.springframework.test.util.ReflectionTestUtils.setField(unit, "id", unitId);
        Listing listing = new Listing(unitId, 2L, "iPhone 15 Pro", "desc", new BigDecimal("10000000.00"), "A+");
        listing.setStatus(Listing.ListingStatus.ACTIVE);

        when(reservationRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(reservation));
        when(productUnitRepository.findByIdForUpdate(unitId)).thenReturn(Optional.of(unit));
        when(listingRepository.findByIdForUpdate(listingId)).thenReturn(Optional.of(listing));
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
        UUID unitId = UUID.randomUUID();
        UnitReservation expiredReservation = new UnitReservation(unitId, 1L, UUID.randomUUID(), Instant.now().minus(2, ChronoUnit.MINUTES));
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN9999", 2L, ProductUnit.UnitStatus.RESERVED, "A+");

        when(reservationRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(expiredReservation));
        when(productUnitRepository.findByIdForUpdate(unitId)).thenReturn(Optional.of(unit));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(token, "GATEWAY", "Address");

        assertThatThrownBy(() -> checkoutSagaService.processPaymentAndSettleOrder(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reservation lease has expired");
        assertThat(unit.getStatus()).isEqualTo(ProductUnit.UnitStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Reject checkout saga when reservation belongs to different buyer")
    void testRejectReservationOwnershipMismatch() {
        UUID token = UUID.randomUUID();
        UnitReservation reservation = new UnitReservation(UUID.randomUUID(), 2L, UUID.randomUUID(), Instant.now().plus(10, ChronoUnit.MINUTES));

        when(reservationRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(reservation));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(token, "GATEWAY", "Address");

        assertThatThrownBy(() -> checkoutSagaService.processPaymentAndSettleOrder(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reservation does not belong to this buyer");
    }

    @Test
    @DisplayName("Return cached response on repeated request with same idempotency key")
    void testIdempotentPaymentConfirmation() throws Exception {
        UUID token = UUID.randomUUID();
        String idempotencyKey = "IDEMP-KEY-123";
        String endpoint = "/api/v1/checkout/confirm-payment";

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(token, "ESCROW", "Address");
        String payload = token + "|ESCROW|Address";
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        String hash = java.util.HexFormat.of().formatHex(digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        OrderConfirmationResponse cachedResponse = new OrderConfirmationResponse(
                UUID.randomUUID(),
                "ORD-CACHED-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10000000.00"),
                new BigDecimal("1500000.00"),
                new BigDecimal("8500000.00"),
                "PAID",
                "HELD",
                Instant.now()
        );

        IdempotencyKeyRecord record = new IdempotencyKeyRecord(1L, idempotencyKey, endpoint, hash, Instant.now().plus(24, ChronoUnit.HOURS));
        record.setResponseStatus(200);
        record.setResponseBody(objectMapper.writeValueAsString(cachedResponse));

        when(idempotencyKeyRecordRepository.findByUserIdAndIdempotencyKeyAndEndpointForUpdate(1L, idempotencyKey, endpoint))
                .thenReturn(Optional.of(record));

        OrderConfirmationResponse response = checkoutSagaService.processPaymentAndSettleOrder(1L, request, idempotencyKey);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo("ORD-CACHED-001");
    }
}
