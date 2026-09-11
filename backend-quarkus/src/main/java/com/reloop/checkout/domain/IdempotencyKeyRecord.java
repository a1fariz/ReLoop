package com.reloop.checkout.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String idempotencyKey;

    @Column(nullable = false, length = 200)
    private String endpoint;

    @Column(nullable = false, length = 128)
    private String requestHash;

    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String responseBody;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    public IdempotencyKeyRecord() {}

    public IdempotencyKeyRecord(Long userId, String idempotencyKey, String endpoint, String requestHash, Instant expiresAt) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.endpoint = endpoint;
        this.requestHash = requestHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getEndpoint() { return endpoint; }
    public String getRequestHash() { return requestHash; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
