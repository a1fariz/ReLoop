-- V12: User profile & KYC columns + KYC transition audit log
-- KYC state lives on users.*; users module writes it via native SQL (no JPA access to auth.User).
-- user_kyc_logs is the users module's own projection-driven audit trail with an FK back to users.

ALTER TABLE users
    ADD COLUMN kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SUBMITTED', 'VERIFIED', 'REJECTED'
    ADD COLUMN kyc_document_type VARCHAR(50),
    ADD COLUMN kyc_document_reference VARCHAR(100),
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN national_id VARCHAR(50),
    ADD COLUMN address JSONB NOT NULL DEFAULT '{}',
    ADD COLUMN kyc_submitted_at TIMESTAMPTZ,
    ADD COLUMN kyc_verified_at TIMESTAMPTZ;

CREATE TABLE user_kyc_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    previous_status VARCHAR(30) NOT NULL,
    new_status VARCHAR(30) NOT NULL,
    reviewed_by BIGINT REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_kyc_logs_user ON user_kyc_logs(user_id);