package com.reloop.disputes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DisputeDtos {
    public record CreateDisputeRequest(
        @NotNull UUID fulfillmentOrderId,
        @NotNull Long sellerId,
        @NotBlank String reason,
        @NotBlank String claimDescription
    ) {}

    public record ResolveDisputeRequest(
        @NotNull String resolutionType, // FULL_REFUND, PARTIAL_REFUND, RELEASE_PAYMENT
        BigDecimal buyerRefundAmount,
        BigDecimal sellerPayoutAmount,
        String resolutionNotes
    ) {}

    public record DisputeResponse(
        UUID id,
        UUID fulfillmentOrderId,
        Long buyerId,
        Long sellerId,
        String reason,
        String claimDescription,
        String status,
        String resolutionType,
        BigDecimal buyerRefundAmount,
        BigDecimal sellerPayoutAmount,
        Instant createdAt
    ) {}
}
