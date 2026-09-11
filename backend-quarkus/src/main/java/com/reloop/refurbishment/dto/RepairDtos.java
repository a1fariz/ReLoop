package com.reloop.refurbishment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RepairDtos {

    public record CreateRepairTicketDto(
        @NotNull UUID unitId,
        @NotBlank @Size(max = 2000) String issueDescription,
        @PositiveOrZero BigDecimal initialPartsCost
    ) {}

    public record ComponentReplacementDto(
        @NotBlank @Size(max = 100) String componentName,
        @Size(max = 100) String serialNumber,
        @NotNull @PositiveOrZero BigDecimal cost
    ) {}

    public record SubmitQcDto(
        @NotNull @Valid List<ComponentReplacementDto> components,
        BigDecimal totalPartsCost
    ) {}

    public record CompleteRepairDto(
        boolean regraded,
        Integer newPhysicalScore,
        Integer newHardwareScore,
        Integer newSoftwareScore,
        @Size(max = 2000) String technicianNotes
    ) {}

    public record CancelRepairDto(
        @NotBlank @Size(max = 1000) String reason
    ) {}

    public record RepairTicketDto(
        UUID id,
        UUID unitId,
        Long technicianId,
        String issueDescription,
        String replacedComponents,
        BigDecimal partsCost,
        String status,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
