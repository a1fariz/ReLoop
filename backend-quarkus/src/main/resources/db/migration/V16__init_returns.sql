-- V16: Return Authorizations (request -> approval -> reverse logistics -> inspection -> refund)

CREATE TABLE return_authorizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fulfillment_order_id UUID NOT NULL REFERENCES fulfillment_orders(id) ON DELETE RESTRICT,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    evidence_images JSONB NOT NULL DEFAULT '[]',
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED', -- 'REQUESTED','APPROVED','REJECTED','IN_TRANSIT','RECEIVED','INSPECTED','REFUNDED','CLOSED'
    rejection_reason VARCHAR(255),
    courier_name VARCHAR(100),
    tracking_number VARCHAR(100),
    inspection_notes TEXT,
    refund_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (refund_amount >= 0),
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_returns_buyer ON return_authorizations(buyer_id);
CREATE INDEX idx_returns_fulfillment ON return_authorizations(fulfillment_order_id);
