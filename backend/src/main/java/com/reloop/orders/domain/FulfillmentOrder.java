package com.reloop.orders.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fulfillment_orders")
public class FulfillmentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID masterOrderId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private UUID unitId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal platformFeeAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal sellerNetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.PROCESSING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus escrowStatus = EscrowStatus.PENDING;

    private String trackingNumber;
    private String courierName;
    private Instant shippedAt;
    private Instant deliveredAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum FulfillmentStatus {
        PROCESSING, SHIPPED, DELIVERED, COMPLETED, DISPUTED, CANCELLED
    }

    public enum EscrowStatus {
        PENDING, HELD, SETTLED, PARTIALLY_REFUNDED, FULLY_REFUNDED
    }

    public FulfillmentOrder() {}

    public FulfillmentOrder(UUID masterOrderId, Long sellerId, UUID unitId, BigDecimal subtotalAmount, BigDecimal platformFeeAmount, BigDecimal sellerNetAmount) {
        this.masterOrderId = masterOrderId;
        this.sellerId = sellerId;
        this.unitId = unitId;
        this.subtotalAmount = subtotalAmount;
        this.platformFeeAmount = platformFeeAmount;
        this.sellerNetAmount = sellerNetAmount;
        this.fulfillmentStatus = FulfillmentStatus.PROCESSING;
        this.escrowStatus = EscrowStatus.PENDING;
    }

    public UUID getId() { return id; }
    public UUID getMasterOrderId() { return masterOrderId; }
    public Long getSellerId() { return sellerId; }
    public UUID getUnitId() { return unitId; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getPlatformFeeAmount() { return platformFeeAmount; }
    public BigDecimal getSellerNetAmount() { return sellerNetAmount; }
    public FulfillmentStatus getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(FulfillmentStatus fulfillmentStatus) { this.fulfillmentStatus = fulfillmentStatus; }
    public EscrowStatus getEscrowStatus() { return escrowStatus; }
    public void setEscrowStatus(EscrowStatus escrowStatus) { this.escrowStatus = escrowStatus; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public Instant getShippedAt() { return shippedAt; }
    public void setShippedAt(Instant shippedAt) { this.shippedAt = shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
