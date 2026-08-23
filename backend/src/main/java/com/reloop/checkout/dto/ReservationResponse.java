package com.reloop.checkout.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
    UUID reservationId,
    UUID unitId,
    UUID listingId,
    UUID token,
    Instant expiresAt,
    long remainingSeconds
) {}
