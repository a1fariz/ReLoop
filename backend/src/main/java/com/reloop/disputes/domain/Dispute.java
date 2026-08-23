package com.reloop.disputes.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes")
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID fulfillmentOrderId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(nullable = false, columnDefinition = "text")
    private String claimDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ResolutionType resolutionType;

    @Column(precision = 15, scale = 2)
    private BigDecimal buyerRefundAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal sellerPayoutAmount = BigDecimal.ZERO;

    private String resolutionNotes;
    private Long resolvedByAdminId;
    private Instant resolvedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum DisputeStatus {
        OPEN, UNDER_REVIEW, EVIDENCE_COLLECTING, RESOLVED, CLOSED
    }

    public enum ResolutionType {
        FULL_REFUND, PARTIAL_REFUND, REPAIR, REPLACEMENT, RELEASE_PAYMENT
    }

    public Dispute() {}

    public Dispute(UUID fulfillmentOrderId, Long buyerId, Long sellerId, String reason, String claimDescription) {
        this.fulfillmentOrderId = fulfillmentOrderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.reason = reason;
        this.claimDescription = claimDescription;
        this.status = DisputeStatus.OPEN;
    }

    public UUID getId() { return id; }
    public UUID getFulfillmentOrderId() { return fulfillmentOrderId; }
    public Long getBuyerId() { return buyerId; }
    public Long getSellerId() { return sellerId; }
    public String getReason() { return reason; }
    public String getClaimDescription() { return claimDescription; }
    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }
    public ResolutionType getResolutionType() { return resolutionType; }
    public void setResolutionType(ResolutionType resolutionType) { this.resolutionType = resolutionType; }
    public BigDecimal getBuyerRefundAmount() { return buyerRefundAmount; }
    public void setBuyerRefundAmount(BigDecimal buyerRefundAmount) { this.buyerRefundAmount = buyerRefundAmount; }
    public BigDecimal getSellerPayoutAmount() { return sellerPayoutAmount; }
    public void setSellerPayoutAmount(BigDecimal sellerPayoutAmount) { this.sellerPayoutAmount = sellerPayoutAmount; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public Long getResolvedByAdminId() { return resolvedByAdminId; }
    public void setResolvedByAdminId(Long resolvedByAdminId) { this.resolvedByAdminId = resolvedByAdminId; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
