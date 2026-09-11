-- V17: In-App Notification Center

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'GENERAL', -- 'GENERAL','ORDER','PAYMENT','ESCROW','TRADE_IN','DISPUTE','RETURN','WARRANTY','SYSTEM'
    reference_type VARCHAR(50),
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_recipient_unread ON notifications(recipient_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, created_at DESC);
