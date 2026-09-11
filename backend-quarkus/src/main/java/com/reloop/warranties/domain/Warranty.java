package com.reloop.warranties.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "warranties")
public class Warranty {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private UUID fulfillmentOrderId;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, length = 50)
    private String policyTier = "STANDARD_6_MONTHS";

    @Column(nullable = false)
    private boolean isVoided = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Warranty() {}

    public Warranty(UUID unitId, Long ownerId, UUID fulfillmentOrderId, Instant startsAt, Instant expiresAt, String policyTier) {
        this.unitId = unitId;
        this.ownerId = ownerId;
        this.fulfillmentOrderId = fulfillmentOrderId;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.policyTier = policyTier != null ? policyTier : "STANDARD_6_MONTHS";
    }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public Long getOwnerId() { return ownerId; }
    public UUID getFulfillmentOrderId() { return fulfillmentOrderId; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getPolicyTier() { return policyTier; }
    public boolean isVoided() { return isVoided; }
    public void setVoided(boolean voided) { isVoided = voided; }
    public Instant getCreatedAt() { return createdAt; }
}
