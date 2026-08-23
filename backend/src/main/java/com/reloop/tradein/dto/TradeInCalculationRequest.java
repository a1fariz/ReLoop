package com.reloop.tradein.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeInCalculationRequest(
    @NotNull BigDecimal msrp,
    @NotNull BigDecimal annualDepreciationRate,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate releaseDate,
    @NotNull String condition,
    @NotNull String functionality,
    int batteryHealthPercentage,
    boolean hasCompleteAccessories,
    BigDecimal estimatedRepairCost
) {}
