package com.reloop.units.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_units")
public class ProductUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productModelId;

    @Column(nullable = false)
    private String serialNumber;

    @Column(nullable = false)
    private Long currentOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "physical_custody_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private PhysicalCustody currentCustody = PhysicalCustody.OWNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "product_unit_status")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private UnitStatus status = UnitStatus.DRAFT;

    private String grade;

    private Integer batteryHealthPercentage;

    @Column(precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum PhysicalCustody {
        OWNER, PLATFORM_HUB, SELLER, LOGISTICS_3PL, BUYER
    }

    public enum UnitStatus {
        DRAFT, AVAILABLE, RESERVED, SOLD, IN_TRANSIT, DELIVERED, OWNED,
        TRADE_IN_PENDING, IN_INSPECTION, IN_REPAIR, REFURBISHED,
        READY_FOR_LISTING, LISTED, RETURN_INSPECTION, RETURNED_TO_OWNER, DAMAGED, DISPOSED
    }

    public ProductUnit() {}

    public ProductUnit(UUID productModelId, String serialNumber, Long currentOwnerId, UnitStatus status, String grade) {
        this.productModelId = productModelId;
        this.serialNumber = serialNumber;
        this.currentOwnerId = currentOwnerId;
        this.status = status != null ? status : UnitStatus.DRAFT;
        this.grade = grade;
    }

    public UUID getId() { return id; }
    public UUID getProductModelId() { return productModelId; }
    public String getSerialNumber() { return serialNumber; }
    public Long getCurrentOwnerId() { return currentOwnerId; }
    public void setCurrentOwnerId(Long currentOwnerId) { this.currentOwnerId = currentOwnerId; }
    public PhysicalCustody getCurrentCustody() { return currentCustody; }
    public void setCurrentCustody(PhysicalCustody currentCustody) { this.currentCustody = currentCustody; }
    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus status) { this.status = status; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Integer getBatteryHealthPercentage() { return batteryHealthPercentage; }
    public void setBatteryHealthPercentage(Integer batteryHealthPercentage) { this.batteryHealthPercentage = batteryHealthPercentage; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
