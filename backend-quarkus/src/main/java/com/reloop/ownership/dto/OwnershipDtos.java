package com.reloop.ownership.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OwnershipDtos {

    public record RecordTransferDto(
        @NotNull UUID unitId,
        Long fromOwnerId,
        @NotNull Long toOwnerId,
        @NotNull String transferType,
        @NotNull @Size(max = 50) String referenceType,
        UUID referenceId
    ) {}

    public record OwnershipTransferDto(
        UUID id,
        UUID unitId,
        Long fromOwnerId,
        Long toOwnerId,
        String transferType,
        String referenceType,
        UUID referenceId,
        Instant transferredAt
    ) {}

    public record OwnershipChainDto(
        UUID unitId,
        List<OwnershipTransferDto> transfers,
        Long currentOwnerId,
        boolean isValid,
        List<String> issues
    ) {}
}
