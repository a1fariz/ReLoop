package com.reloop.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentDtos {
    public record InitiatePaymentDto(
        @NotNull UUID masterOrderId
    ) {}

    public record WebhookDto(
        @NotBlank @Size(max = 100) String gatewayReference,
        @NotBlank @Pattern(regexp = "SUCCEEDED|FAILED",
                message = "status must be SUCCEEDED or FAILED") String status,
        @Size(max = 255) String failureReason
    ) {}

    public record PaymentAttemptDto(
        UUID id,
        UUID masterOrderId,
        Long buyerId,
        BigDecimal amount,
        String currency,
        String gateway,
        String gatewayReference,
        String status,
        String failureReason,
        Instant initiatedAt,
        Instant completedAt,
        Instant createdAt
    ) {}
}
