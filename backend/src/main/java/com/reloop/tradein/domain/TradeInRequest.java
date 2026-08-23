package com.reloop.tradein.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trade_in_requests")
public class TradeInRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private UUID productModelId;

    @Column(nullable = false, length = 30)
    private String declaredCondition;

    @Column(nullable = false, length = 30)
    private String declaredFunctionality;

    private Integer declaredBatteryHealth;

    @Column(nullable = false)
    private boolean hasCompleteAccessories = false;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedOffer;

    @Column(precision = 15, scale = 2)
    private BigDecimal finalCounterOffer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TradeInStatus status = TradeInStatus.SUBMITTED;

    private UUID unitId;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum TradeInStatus {
        SUBMITTED, IN_TRANSIT, INSPECTING, COUNTER_OFFER_PENDING, ACCEPTED, REJECTED, COMPLETED
    }

    public TradeInRequest() {}

    public TradeInRequest(Long userId, UUID productModelId, String declaredCondition, String declaredFunctionality, Integer declaredBatteryHealth, boolean hasCompleteAccessories, BigDecimal estimatedOffer) {
        this.userId = userId;
        this.productModelId = productModelId;
        this.declaredCondition = declaredCondition;
        this.declaredFunctionality = declaredFunctionality;
        this.declaredBatteryHealth = declaredBatteryHealth;
        this.hasCompleteAccessories = hasCompleteAccessories;
        this.estimatedOffer = estimatedOffer;
        this.status = TradeInStatus.SUBMITTED;
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public UUID getProductModelId() { return productModelId; }
    public String getDeclaredCondition() { return declaredCondition; }
    public String getDeclaredFunctionality() { return declaredFunctionality; }
    public Integer getDeclaredBatteryHealth() { return declaredBatteryHealth; }
    public boolean isHasCompleteAccessories() { return hasCompleteAccessories; }
    public BigDecimal getEstimatedOffer() { return estimatedOffer; }
    public BigDecimal getFinalCounterOffer() { return finalCounterOffer; }
    public void setFinalCounterOffer(BigDecimal finalCounterOffer) { this.finalCounterOffer = finalCounterOffer; }
    public TradeInStatus getStatus() { return status; }
    public void setStatus(TradeInStatus status) { this.status = status; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
