package com.reloop.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FulfillmentOrderDto(
    UUID id,
    UUID masterOrderId,
    Long sellerId,
    UUID unitId,
    BigDecimal subtotalAmount,
    BigDecimal platformFeeAmount,
    BigDecimal sellerNetAmount,
    String fulfillmentStatus,
    String escrowStatus,
    String trackingNumber,
    String courierName,
    Instant shippedAt,
    Instant deliveredAt,
    Instant createdAt
) {}
