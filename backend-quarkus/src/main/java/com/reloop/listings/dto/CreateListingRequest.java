package com.reloop.listings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequest(
    @NotNull UUID unitId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String description,
    @NotNull @DecimalMin(value = "0.01") BigDecimal askingPrice,
    @Size(max = 10000) String images
) {}
