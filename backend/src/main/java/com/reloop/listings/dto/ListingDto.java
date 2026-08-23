package com.reloop.listings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingDto(
    UUID id,
    UUID unitId,
    Long sellerId,
    String title,
    String description,
    BigDecimal askingPrice,
    String status,
    String gradeSnapshot
) {}
