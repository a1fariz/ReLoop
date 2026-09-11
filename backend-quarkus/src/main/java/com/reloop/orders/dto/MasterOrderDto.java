package com.reloop.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MasterOrderDto(
    UUID id,
    String orderNumber,
    Long buyerId,
    BigDecimal totalAmount,
    String paymentStatus,
    Instant createdAt
) {}
