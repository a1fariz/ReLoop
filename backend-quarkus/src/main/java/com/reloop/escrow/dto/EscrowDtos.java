package com.reloop.escrow.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EscrowDtos {
    public record EscrowSummaryDto(
        UUID fulfillmentOrderId,
        UUID masterOrderId,
        Long sellerId,
        String escrowStatus,
        BigDecimal escrowHoldAmount,
        BigDecimal platformFee,
        BigDecimal sellerNet,
        boolean disputeOpen,
        List<JournalDto> journals
    ) {}

    public record JournalDto(
        UUID id,
        String referenceType,
        String referenceId,
        String description,
        Instant createdAt
    ) {}

    public record EscrowStatsDto(
        Map<String, Long> escrowStatusCounts,
        BigDecimal totalHeldAmount
    ) {}
}
