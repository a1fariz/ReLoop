package com.reloop.checkout.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReserveUnitRequest(
    @NotNull UUID unitId,
    @NotNull UUID listingId
) {}
