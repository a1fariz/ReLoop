-- V3: Listings, Anti-Hoarding Reservations & Idempotency Keys

CREATE TABLE listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id) ON DELETE RESTRICT,
    seller_id BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    asking_price NUMERIC(15,2) NOT NULL CHECK (asking_price > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'PAUSED', 'SOLD', 'REMOVED'
    grade_snapshot VARCHAR(5) NOT NULL,
    images JSONB NOT NULL DEFAULT '[]',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Invariant: Exactly 1 active listing per unit
CREATE UNIQUE INDEX uq_single_active_listing_per_unit 
ON listings (unit_id) 
WHERE status = 'ACTIVE';

CREATE TABLE unit_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE RESTRICT,
    token UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'CONVERTED', 'EXPIRED', 'CANCELLED'
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reservation_ttl CHECK (expires_at > created_at)
);

-- Partial Unique Index: Guarantees exactly 1 active reservation lease per unit
CREATE UNIQUE INDEX uq_single_active_unit_reservation 
ON unit_reservations (unit_id) 
WHERE status = 'ACTIVE';

CREATE INDEX idx_reservations_expiration 
ON unit_reservations (status, expires_at) 
WHERE status = 'ACTIVE';

-- Idempotency Keys Table for Financial & Mutation Safety
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(150) NOT NULL,
    endpoint VARCHAR(200) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_status INT,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_idempotency_endpoint UNIQUE (user_id, idempotency_key, endpoint)
);
