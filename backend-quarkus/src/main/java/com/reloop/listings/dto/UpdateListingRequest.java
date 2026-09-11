package com.reloop.listings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Partial update: null fields are left unchanged. */
public record UpdateListingRequest(
    @Size(max = 255) String title,
    @Size(max = 2000) String description,
    @DecimalMin(value = "0.01") BigDecimal askingPrice,
    @Size(max = 10000) String images
) {}
