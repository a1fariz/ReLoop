-- V1: Authentication, Users, Roles, KYC and Refresh Tokens

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30),
    role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER', -- 'CUSTOMER', 'SELLER', 'TECHNICIAN', 'ADMIN'
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);

CREATE TABLE sellers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    store_name VARCHAR(150) NOT NULL UNIQUE,
    store_slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    reputation_score NUMERIC(5,2) NOT NULL DEFAULT 50.00 CHECK (reputation_score >= 0 AND reputation_score <= 100),
    return_rate NUMERIC(4,3) NOT NULL DEFAULT 0.000 CHECK (return_rate >= 0 AND return_rate <= 1),
    dispute_rate NUMERIC(4,3) NOT NULL DEFAULT 0.000 CHECK (dispute_rate >= 0 AND dispute_rate <= 1),
    response_rate NUMERIC(4,3) NOT NULL DEFAULT 1.000 CHECK (response_rate >= 0 AND response_rate <= 1),
    completed_orders INT NOT NULL DEFAULT 0 CHECK (completed_orders >= 0),
    active_disputes INT NOT NULL DEFAULT 0 CHECK (active_disputes >= 0),
    kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'VERIFIED', 'REJECTED'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sellers_slug ON sellers(store_slug);
