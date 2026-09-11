package com.reloop.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReturnDtos {
    public record CreateReturnRequest(
            @NotNull UUID fulfillmentOrderId,
            @NotBlank @Size(max = 100) String reason,
            @NotBlank @Size(max = 2000) String description,
            List<String> evidenceImages
    ) {}

    public record ReviewReturnRequest(
            @NotNull Boolean approved,
            @Size(max = 255) String reason
    ) {}

    public record ShipmentRequest(
            @NotBlank @Size(max = 100) String courierName,
            @NotBlank @Size(max = 100) String trackingNumber
    ) {}

    public record InspectionRequest(
            @Size(max = 2000) String notes,
            @NotNull @PositiveOrZero BigDecimal refundAmount
    ) {}

    public record ReturnResponse(
            UUID id,
            UUID fulfillmentOrderId,
            Long buyerId,
            String reason,
            String description,
            List<String> evidenceImages,
            String status,
            String rejectionReason,
            String courierName,
            String trackingNumber,
            String inspectionNotes,
            BigDecimal refundAmount,
            Instant resolvedAt,
            Instant createdAt
    ) {}
}
