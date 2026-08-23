package com.reloop.checkout.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmationResponse(
    UUID masterOrderId,
    String orderNumber,
    UUID fulfillmentOrderId,
    UUID unitId,
    BigDecimal totalAmount,
    BigDecimal platformFeeAmount,
    BigDecimal sellerNetAmount,
    String paymentStatus,
    String escrowStatus,
    Instant createdAt
) {}
