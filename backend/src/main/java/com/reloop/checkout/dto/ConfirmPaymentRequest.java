package com.reloop.checkout.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConfirmPaymentRequest(
    @NotNull UUID reservationToken,
    @NotNull String paymentMethod, // 'SIMULATED_ESCROW_DIRECT', 'VIRTUAL_ACCOUNT', 'QRIS'
    @NotNull String shippingAddress
) {}
