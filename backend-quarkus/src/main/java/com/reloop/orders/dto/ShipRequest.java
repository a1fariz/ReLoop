package com.reloop.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipRequest(
    @NotBlank @Size(max = 64) String trackingNumber,
    @NotBlank @Size(max = 64) String courierName
) {}
