package com.reloop.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ReviewDtos {

    public record SubmitReviewRequest(
        @NotNull UUID fulfillmentOrderId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 2000) String comment
    ) {}

    public record ReviewResponse(
        UUID id,
        UUID fulfillmentOrderId,
        UUID unitId,
        Long sellerId,
        Long buyerId,
        int rating,
        String comment,
        java.time.Instant createdAt
    ) {}
}
