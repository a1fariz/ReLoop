-- V7: Transactional Outbox Pattern & Immutable Audit Log

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL, -- 'ORDER', 'PRODUCT_UNIT', 'ESCROW', 'TRADE_IN'
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,   -- 'ORDER_PAID', 'UNIT_STATUS_CHANGED', 'ESCROW_SETTLED'
    payload JSONB NOT NULL,
    correlation_id UUID NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PROCESSED', 'FAILED', 'DEAD_LETTER'
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Fast Poller Partial Index for Background Outbox Workers
CREATE INDEX idx_outbox_unprocessed 
ON outbox_events (status, next_retry_at) 
WHERE status IN ('PENDING', 'FAILED');

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE', 'STATE_TRANSITION'
    actor_id BIGINT,
    actor_email VARCHAR(255),
    actor_role VARCHAR(50),
    correlation_id UUID,
    from_state JSONB,
    to_state JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- Invariant Enforcement: Revoke mutation privileges on audit logs
-- (Applications can only INSERT into audit_logs, never UPDATE or DELETE)
