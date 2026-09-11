package com.reloop.tradein.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class TradeInDtos {

    public record SubmitTradeInRequest(
        @NotNull UUID productModelId,
        @NotNull BigDecimal msrp,
        @NotNull @DecimalMin("0.001") @DecimalMax("0.999") BigDecimal annualDepreciationRate,
        @NotNull LocalDate releaseDate,
        @NotNull @Size(max = 30) String declaredCondition,
        @NotNull @Size(max = 30) String declaredFunctionality,
        @Min(0) @Max(100) Integer batteryHealthPercentage,
        boolean hasCompleteAccessories,
        BigDecimal estimatedRepairCost
    ) {}

    public record TradeInRequestResponse(
        UUID id,
        UUID productModelId,
        String declaredCondition,
        String declaredFunctionality,
        Integer declaredBatteryHealth,
        boolean hasCompleteAccessories,
        BigDecimal estimatedOffer,
        String status,
        java.time.Instant createdAt
    ) {}
}
