package com.reloop.users.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * users-module-owned audit trail of KYC state transitions on users.* (written
 * via native SQL in UserKycService because the users table is owned by the
 * auth module's User entity).
 */
@Entity
@Table(name = "user_kyc_logs")
public class UserKycLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String previousStatus;

    @Column(nullable = false, length = 30)
    private String newStatus;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UserKycLog() {}

    public UserKycLog(Long userId, String previousStatus, String newStatus, Long reviewedBy, String notes) {
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewedBy = reviewedBy;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public Long getReviewedBy() { return reviewedBy; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
