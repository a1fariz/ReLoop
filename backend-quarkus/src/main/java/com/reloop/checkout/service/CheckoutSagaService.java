package com.reloop.checkout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.checkout.domain.IdempotencyKeyRecord;
import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.dto.ConfirmPaymentRequest;
import com.reloop.checkout.dto.OrderConfirmationResponse;
import com.reloop.checkout.repository.IdempotencyKeyRecordRepository;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.repository.ListingRepository;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CheckoutSagaService {

    private final UnitReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final ProductUnitRepository productUnitRepository;
    private final MasterOrderRepository masterOrderRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final DoubleEntryLedgerService ledgerService;
    private final IdempotencyKeyRecordRepository idempotencyKeyRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final BigDecimal platformFeeRate;
    private final MeterRegistry meterRegistry;
    private final UUID instanceCorrelationId = UUID.randomUUID();

    @Inject
    public CheckoutSagaService(
            UnitReservationRepository reservationRepository,
            ListingRepository listingRepository,
            ProductUnitRepository productUnitRepository,
            MasterOrderRepository masterOrderRepository,
            FulfillmentOrderRepository fulfillmentOrderRepository,
            DoubleEntryLedgerService ledgerService,
            IdempotencyKeyRecordRepository idempotencyKeyRecordRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @org.eclipse.microprofile.config.inject.ConfigProperty(name = "reloop.checkout.platform-fee-rate", defaultValue = "0.15") BigDecimal platformFeeRate,
            io.micrometer.core.instrument.MeterRegistry meterRegistry
    ) {
        this.reservationRepository = reservationRepository;
        this.listingRepository = listingRepository;
        this.productUnitRepository = productUnitRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.ledgerService = ledgerService;
        this.idempotencyKeyRecordRepository = idempotencyKeyRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.platformFeeRate = platformFeeRate;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public OrderConfirmationResponse processPaymentAndSettleOrder(Long buyerId, ConfirmPaymentRequest request) {
        return processPaymentAndSettleOrder(buyerId, request, null, null);
    }

    @Transactional
    public OrderConfirmationResponse processPaymentAndSettleOrder(Long buyerId, ConfirmPaymentRequest request, String idempotencyKey) {
        return processPaymentAndSettleOrder(buyerId, request, idempotencyKey, null);
    }

    @Transactional
    public OrderConfirmationResponse processPaymentAndSettleOrder(Long buyerId, ConfirmPaymentRequest request,
                                                                  String idempotencyKey, String buyerEmail) {
        final String endpoint = "/api/v1/checkout/confirm-payment";
        IdempotencyKeyRecord idempotencyRecord = null;

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String requestPayload = request != null ? request.getReservationToken() + "|" + request.getPaymentMethod() + "|" + request.getShippingAddress() : "";
            String requestHash = sha256(requestPayload);

            Optional<IdempotencyKeyRecord> existingKeyOpt = idempotencyKeyRecordRepository
                    .findByUserIdAndIdempotencyKeyAndEndpointForUpdate(buyerId, idempotencyKey, endpoint);

            if (existingKeyOpt.isPresent()) {
                IdempotencyKeyRecord existingKey = existingKeyOpt.get();
                if (!existingKey.getRequestHash().equals(requestHash)) {
                    throw new BusinessException("Idempotency key payload mismatch", "IDEMPOTENCY_CONFLICT", 409);
                }
                if (existingKey.getResponseBody() != null) {
                    try {
                        return objectMapper.readValue(existingKey.getResponseBody(), OrderConfirmationResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new BusinessException("Failed to deserialize cached response", "IDEMPOTENCY_ERROR", 500);
                    }
                }
            } else {
                idempotencyRecord = new IdempotencyKeyRecord(buyerId, idempotencyKey, endpoint, requestHash, Instant.now().plus(24, ChronoUnit.HOURS));
                idempotencyRecord = idempotencyKeyRecordRepository.save(idempotencyRecord);
            }
        }
        // Step 1: Validate active 15-min reservation lease
        UnitReservation reservation = reservationRepository.findByTokenForUpdate(request.getReservationToken())
                .orElseThrow(() -> new BusinessException("Invalid reservation token", "RESERVATION_NOT_FOUND", 404));

        if (!reservation.getUserId().equals(buyerId)) {
            throw new BusinessException("Reservation does not belong to this buyer", "RESERVATION_FORBIDDEN", 403);
        }

        // Step 2: Acquire row lock on unit & listing
        ProductUnit unit = productUnitRepository.findByIdForUpdate(reservation.getUnitId())
                .orElseThrow(() -> new BusinessException("Unit not found", "UNIT_NOT_FOUND", 404));

        if (reservation.getStatus() != UnitReservation.ReservationStatus.ACTIVE || !reservation.getExpiresAt().isAfter(Instant.now())) {
            // Note: any compensation mutation here would be rolled back together with
            // the exception — the ReservationExpiryReaperWorker is the authoritative
            // mechanism that restores the unit to AVAILABLE.
            throw new BusinessException("Reservation lease has expired. The unit will be returned to the marketplace shortly.", "LEASE_EXPIRED", 410);
        }

        Listing listing = listingRepository.findByIdForUpdate(reservation.getListingId())
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", 404));
        if (!listing.getUnitId().equals(unit.getId()) || listing.getStatus() != Listing.ListingStatus.ACTIVE || unit.getStatus() != ProductUnit.UnitStatus.RESERVED) {
            throw new BusinessException("Reserved listing is no longer available", "LISTING_NOT_ACTIVE", 409);
        }

        // Step 3: Compute exact server-authoritative financial breakdown
        BigDecimal totalAmount = listing.getAskingPrice();
        BigDecimal platformFee = totalAmount.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerNet = totalAmount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

        // Step 4: Create Master Order & Fulfillment Sub-Order
        String orderNumber = "ORD-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MasterOrder masterOrder = new MasterOrder(orderNumber, buyerId, totalAmount, normalizeShippingAddress(request.getShippingAddress()));
        masterOrder.setPaymentStatus(MasterOrder.PaymentStatus.PAID);
        masterOrder = masterOrderRepository.save(masterOrder);

        FulfillmentOrder fulfillment = new FulfillmentOrder(
                masterOrder.getId(),
                listing.getSellerId(),
                unit.getId(),
                totalAmount,
                platformFee,
                sellerNet
        );
        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.HELD);
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.PROCESSING);
        fulfillment = fulfillmentOrderRepository.save(fulfillment);

        // Step 5: Post Double-Entry Financial Journal (Lock Buyer Funds into Escrow)
        // DR: GATEWAY_CLEARING, CR: ESCROW_HELD (Balanced)
        ledgerService.postJournal(
                "ORDER_PAYMENT",
                masterOrder.getOrderNumber(),
                "Escrow lock for order " + masterOrder.getOrderNumber(),
                List.of(
                        new DoubleEntryLedgerService.PostingLine("GATEWAY_CLEARING", FinancialLedgerLine.EntryType.DR, totalAmount),
                        new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.CR, totalAmount)
                )
        );

        // Step 6: Convert reservation & Mark unit & listing as SOLD
        reservation.setStatus(UnitReservation.ReservationStatus.CONVERTED);
        reservationRepository.save(reservation);

        unit.setStatus(ProductUnit.UnitStatus.SOLD);
        unit.setCurrentCustody(ProductUnit.PhysicalCustody.LOGISTICS_3PL);
        productUnitRepository.save(unit);

        listing.setStatus(Listing.ListingStatus.SOLD);
        listingRepository.save(listing);

        // First-class outbox producer: ORDER_PAID drives async side effects (analytics);
        // the buyer confirmation email is emitted as its own EMAIL outbox event.
        emitOrderPaid(masterOrder, fulfillment, buyerId);
        meterRegistry.counter("reloop.checkout.saga.completed").increment();
        if (buyerEmail != null && !buyerEmail.isBlank()) {
            emitOrderConfirmationEmail(masterOrder, buyerEmail, totalAmount);
        }

        OrderConfirmationResponse response = new OrderConfirmationResponse(
                masterOrder.getId(),
                masterOrder.getOrderNumber(),
                fulfillment.getId(),
                unit.getId(),
                totalAmount,
                platformFee,
                sellerNet,
                masterOrder.getPaymentStatus().name(),
                fulfillment.getEscrowStatus().name(),
                masterOrder.getCreatedAt()
        );

        if (idempotencyRecord != null) {
            try {
                idempotencyRecord.setResponseStatus(Response.Status.OK.getStatusCode());
                idempotencyRecord.setResponseBody(objectMapper.writeValueAsString(response));
                idempotencyKeyRecordRepository.save(idempotencyRecord);
            } catch (JsonProcessingException e) {
                // Non-blocking for response return
            }
        }

        return response;
    }

    /**
     * master_orders.shipping_address is a jsonb column: plain strings are wrapped
     * as JSON strings, structured JSON objects are stored as-is. Prevents driver
     * 500s on free-form addresses.
     */
    private String normalizeShippingAddress(String address) {
        if (address == null || address.isBlank()) {
            return "null";
        }
        try {
            com.fasterxml.jackson.databind.JsonNode tree = objectMapper.readTree(address);
            if (tree.isObject() || tree.isArray()) {
                return address;
            }
        } catch (JsonProcessingException ignored) {
            // plain text address -> wrapped below
        }
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Invalid shipping address", "INVALID_SHIPPING_ADDRESS", 400);
        }
    }

    private void emitOrderPaid(MasterOrder masterOrder, FulfillmentOrder fulfillment, Long buyerId) {
        try {
            String payload = objectMapper.writeValueAsString(new OrderPaidPayload(
                    masterOrder.getId(),
                    masterOrder.getOrderNumber(),
                    buyerId,
                    fulfillment.getSellerId(),
                    fulfillment.getUnitId(),
                    fulfillment.getSubtotalAmount(),
                    fulfillment.getPlatformFeeAmount(),
                    fulfillment.getSellerNetAmount()
            ));
            outboxEventRepository.save(new OutboxEvent(
                    "ORDER",
                    masterOrder.getOrderNumber(),
                    "ORDER_PAID",
                    payload,
                    instanceCorrelationId,
                    "ORDER_PAID:" + masterOrder.getId()
            ));
        } catch (JsonProcessingException e) {
            // Non-fatal: the financial state is committed regardless of event serialization
        }
    }

    record OrderPaidPayload(
            java.util.UUID masterOrderId,
            String orderNumber,
            Long buyerId,
            Long sellerId,
            java.util.UUID unitId,
            BigDecimal totalAmount,
            BigDecimal platformFeeAmount,
            BigDecimal sellerNetAmount
    ) {}

    /** Buyer confirmation email: consumed by OutboxEventProcessor via quarkus-mailer. */
    private void emitOrderConfirmationEmail(MasterOrder masterOrder, String buyerEmail, BigDecimal totalAmount) {
        try {
            String payload = objectMapper.writeValueAsString(new EmailPayload(
                    buyerEmail,
                    "ReLoop: Order " + masterOrder.getOrderNumber() + " confirmed",
                    "Your payment for order " + masterOrder.getOrderNumber() + " has been received and "
                            + "IDR " + totalAmount.toPlainString() + " is held in escrow. The seller is preparing your item."
            ));
            outboxEventRepository.save(new OutboxEvent(
                    "EMAIL",
                    masterOrder.getOrderNumber(),
                    "EMAIL_ORDER_CONFIRMATION",
                    payload,
                    instanceCorrelationId,
                    "EMAIL_ORDER_CONFIRMATION:" + masterOrder.getId()
            ));
        } catch (JsonProcessingException e) {
            // Non-fatal: the order is committed; the email retry path is lost only on serialization failure
        }
    }

    record EmailPayload(String to, String subject, String body) {}

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
