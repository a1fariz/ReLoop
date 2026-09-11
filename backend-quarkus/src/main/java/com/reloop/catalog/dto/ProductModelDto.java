package com.reloop.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductModelDto(
    UUID id,
    Integer categoryId,
    String brand,
    String modelName,
    String slug,
    BigDecimal msrp,
    BigDecimal annualDepreciationRate,
    LocalDate releaseDate
) {}
