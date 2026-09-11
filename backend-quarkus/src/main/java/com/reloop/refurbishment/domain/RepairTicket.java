package com.reloop.refurbishment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repair_tickets")
public class RepairTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private Long technicianId;

    @Column(nullable = false, columnDefinition = "text")
    private String issueDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String replacedComponents = "[]";

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.OPEN;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum Status {
        OPEN, DIAGNOSING, IN_PROGRESS, QC_PENDING, COMPLETED, CANCELLED
    }

    public RepairTicket() {}

    public RepairTicket(UUID unitId, Long technicianId, String issueDescription, BigDecimal partsCost) {
        this.unitId = unitId;
        this.technicianId = technicianId;
        this.issueDescription = issueDescription;
        this.partsCost = partsCost != null ? partsCost : BigDecimal.ZERO;
        this.status = Status.OPEN;
    }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public Long getTechnicianId() { return technicianId; }
    public String getIssueDescription() { return issueDescription; }
    public String getReplacedComponents() { return replacedComponents; }
    public void setReplacedComponents(String replacedComponents) { this.replacedComponents = replacedComponents; }
    public BigDecimal getPartsCost() { return partsCost; }
    public void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
