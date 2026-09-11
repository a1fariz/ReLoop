package com.reloop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CartDtos {
    public record AddCartItemDto(
        @NotNull UUID listingId,
        @Min(1) Integer quantity
    ) {}

    public record CartItemDto(
        UUID cartItemId,
        UUID listingId,
        UUID unitId,
        BigDecimal price,
        int quantity,
        String listingTitle,
        String listingStatus
    ) {}

    public record CartDto(
        List<CartItemDto> items,
        int totalItems,
        int distinctListings
    ) {}
}
