package com.reloop.returns.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_authorizations")
public class ReturnAuthorization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID fulfillmentOrderId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String evidenceImages = "[]";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.REQUESTED;

    @Column(length = 255)
    private String rejectionReason;

    @Column(length = 100)
    private String courierName;

    @Column(length = 100)
    private String trackingNumber;

    @Column(columnDefinition = "text")
    private String inspectionNotes;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    private Instant resolvedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum Status {
        REQUESTED, APPROVED, REJECTED, IN_TRANSIT, RECEIVED, INSPECTED, REFUNDED, CLOSED
    }

    /** States that block a second open return for the same fulfillment. */
    public static Status[] openStatuses() {
        return new Status[]{Status.REQUESTED, Status.APPROVED, Status.IN_TRANSIT, Status.RECEIVED, Status.INSPECTED};
    }

    public ReturnAuthorization() {}

    public ReturnAuthorization(UUID fulfillmentOrderId, Long buyerId, String reason, String description, String evidenceImages) {
        this.fulfillmentOrderId = fulfillmentOrderId;
        this.buyerId = buyerId;
        this.reason = reason;
        this.description = description;
        this.evidenceImages = evidenceImages != null ? evidenceImages : "[]";
        this.status = Status.REQUESTED;
    }

    public UUID getId() { return id; }
    public UUID getFulfillmentOrderId() { return fulfillmentOrderId; }
    public Long getBuyerId() { return buyerId; }
    public String getReason() { return reason; }
    public String getDescription() { return description; }
    public String getEvidenceImages() { return evidenceImages; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getInspectionNotes() { return inspectionNotes; }
    public void setInspectionNotes(String inspectionNotes) { this.inspectionNotes = inspectionNotes; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
