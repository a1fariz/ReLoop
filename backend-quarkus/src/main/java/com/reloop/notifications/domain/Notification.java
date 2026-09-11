package com.reloop.notifications.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category = Category.GENERAL;

    @Column(length = 50)
    private String referenceType;

    private UUID referenceId;

    @Column(nullable = false)
    private boolean isRead = false;

    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum Category {
        GENERAL, ORDER, PAYMENT, ESCROW, TRADE_IN, DISPUTE, RETURN, WARRANTY, SYSTEM
    }

    public Notification() {}

    public Notification(Long recipientId, Category category, String title, String body,
                        String referenceType, UUID referenceId) {
        this.recipientId = recipientId;
        this.category = category != null ? category : Category.GENERAL;
        this.title = title;
        this.body = body;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public UUID getId() { return id; }
    public Long getRecipientId() { return recipientId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Category getCategory() { return category; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
