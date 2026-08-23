package com.reloop.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String entityName;

    @Column(nullable = false, length = 100)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, STATE_TRANSITION

    private Long actorId;
    private String actorEmail;
    private String actorRole;
    private java.util.UUID correlationId;

    @Column(columnDefinition = "jsonb")
    private String fromState;

    @Column(columnDefinition = "jsonb")
    private String toState;

    private String ipAddress;
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditLog() {}

    public AuditLog(String entityName, String entityId, String action, Long actorId, String actorEmail, String actorRole, java.util.UUID correlationId, String fromState, String toState) {
        this.entityName = entityName;
        this.entityId = entityId;
        this.action = action;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.correlationId = correlationId;
        this.fromState = fromState;
        this.toState = toState;
    }

    public Long getId() { return id; }
    public String getEntityName() { return entityName; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public Long getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getActorRole() { return actorRole; }
    public java.util.UUID getCorrelationId() { return correlationId; }
    public String getFromState() { return fromState; }
    public String getToState() { return toState; }
    public Instant getCreatedAt() { return createdAt; }
}
