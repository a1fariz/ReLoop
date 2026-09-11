package com.reloop.warranties.dto;

import java.time.Instant;
import java.util.UUID;

public record WarrantyDto(
    UUID id,
    UUID unitId,
    Long ownerId,
    UUID fulfillmentOrderId,
    Instant startsAt,
    Instant expiresAt,
    String policyTier,
    boolean isVoided
) {}
