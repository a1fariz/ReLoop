package com.reloop.checkout.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unit_reservations")
public class UnitReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private UUID listingId;

    @Column(nullable = false, unique = true)
    private UUID token = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum ReservationStatus {
        ACTIVE, CONVERTED, EXPIRED, CANCELLED
    }

    public UnitReservation() {}

    public UnitReservation(UUID unitId, Long userId, UUID listingId, Instant expiresAt) {
        this.unitId = unitId;
        this.userId = userId;
        this.listingId = listingId;
        this.expiresAt = expiresAt;
        this.token = UUID.randomUUID();
        this.status = ReservationStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public Long getUserId() { return userId; }
    public UUID getListingId() { return listingId; }
    public UUID getToken() { return token; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
