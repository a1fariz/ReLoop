package com.reloop.inspections.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class InspectionDtos {

    public record CreateInspectionRequest(
        @NotNull UUID unitId,
        @NotNull @Min(0) @Max(100) Integer physicalScore,
        @NotNull @Min(0) @Max(100) Integer hardwareScore,
        @NotNull @Min(0) @Max(100) Integer softwareScore,
        boolean hasCriticalFailure,
        BigDecimal estimatedRepairCost,
        @Size(max = 2000) String technicianNotes
    ) {}

    public record InspectionResponse(
        UUID id,
        UUID unitId,
        Long technicianId,
        int physicalScore,
        int hardwareScore,
        int softwareScore,
        String finalCalculatedGrade,
        BigDecimal estimatedRepairCost,
        String technicianNotes,
        java.time.Instant createdAt
    ) {}
}
