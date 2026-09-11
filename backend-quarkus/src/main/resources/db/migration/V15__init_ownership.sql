-- V15: Legal ownership provenance chain (append-only transfer ledger per unit)

CREATE TABLE ownership_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id) ON DELETE RESTRICT,
    from_owner_id BIGINT REFERENCES users(id),
    to_owner_id BIGINT NOT NULL REFERENCES users(id),
    transfer_type VARCHAR(30) NOT NULL, -- 'PURCHASE','TRADE_IN','RETURN','PLATFORM_RECOVERY'
    reference_type VARCHAR(50) NOT NULL, -- 'FULFILLMENT_ORDER','TRADE_IN_REQUEST','RETURN'
    reference_id UUID,
    transferred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ownership_unit ON ownership_transfers(unit_id, transferred_at);
CREATE INDEX idx_ownership_to_owner ON ownership_transfers(to_owner_id);
