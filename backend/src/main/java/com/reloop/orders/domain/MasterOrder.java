package com.reloop.orders.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "master_orders")
public class MasterOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderNumber;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String shippingAddress;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum PaymentStatus {
        PENDING, PROCESSING, PAID, FAILED, CANCELLED
    }

    public MasterOrder() {}

    public MasterOrder(String orderNumber, Long buyerId, BigDecimal totalAmount, String shippingAddress) {
        this.orderNumber = orderNumber;
        this.buyerId = buyerId;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    public UUID getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public Long getBuyerId() { return buyerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getShippingAddress() { return shippingAddress; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
