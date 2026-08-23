package com.reloop.inspections.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "technical_inspections")
public class TechnicalInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private Long technicianId;

    private UUID tradeInRequestId;

    @Column(nullable = false, length = 20)
    private String templateVersion = "1.0";

    @Column(nullable = false)
    private int physicalScore;

    @Column(nullable = false)
    private int hardwareScore;

    @Column(nullable = false)
    private int softwareScore;

    @Column(nullable = false, length = 5)
    private String finalCalculatedGrade;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedRepairCost = BigDecimal.ZERO;

    private String technicianNotes;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String checklistResults = "{}";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TechnicalInspection() {}

    public TechnicalInspection(UUID unitId, Long technicianId, UUID tradeInRequestId, int physicalScore, int hardwareScore, int softwareScore, String finalCalculatedGrade, BigDecimal estimatedRepairCost, String technicianNotes) {
        this.unitId = unitId;
        this.technicianId = technicianId;
        this.tradeInRequestId = tradeInRequestId;
        this.physicalScore = physicalScore;
        this.hardwareScore = hardwareScore;
        this.softwareScore = softwareScore;
        this.finalCalculatedGrade = finalCalculatedGrade;
        this.estimatedRepairCost = estimatedRepairCost != null ? estimatedRepairCost : BigDecimal.ZERO;
        this.technicianNotes = technicianNotes;
    }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public Long getTechnicianId() { return technicianId; }
    public UUID getTradeInRequestId() { return tradeInRequestId; }
    public int getPhysicalScore() { return physicalScore; }
    public int getHardwareScore() { return hardwareScore; }
    public int getSoftwareScore() { return softwareScore; }
    public String getFinalCalculatedGrade() { return finalCalculatedGrade; }
    public BigDecimal getEstimatedRepairCost() { return estimatedRepairCost; }
    public String getTechnicianNotes() { return technicianNotes; }
    public Instant getCreatedAt() { return createdAt; }
}
