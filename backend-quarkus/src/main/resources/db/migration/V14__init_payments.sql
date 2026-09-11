-- V14: Payment Attempts (payment intents with mock gateway + webhook support)

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    master_order_id UUID NOT NULL REFERENCES master_orders(id) ON DELETE RESTRICT,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    gateway VARCHAR(30) NOT NULL DEFAULT 'MOCK_GATEWAY',
    gateway_reference VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED', -- 'INITIATED','PROCESSING','SUCCEEDED','FAILED','EXPIRED'
    failure_reason VARCHAR(255),
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_attempts_order ON payment_attempts(master_order_id);
CREATE INDEX idx_payment_attempts_buyer ON payment_attempts(buyer_id);
