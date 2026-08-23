-- V4: Master Orders & Multi-Seller Fulfillment Sub-Orders

CREATE TABLE master_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(64) NOT NULL UNIQUE,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total_amount NUMERIC(15,2) NOT NULL CHECK (total_amount >= 0),
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED'
    shipping_address JSONB NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_master_orders_buyer ON master_orders(buyer_id);

CREATE TABLE fulfillment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    master_order_id UUID NOT NULL REFERENCES master_orders(id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    unit_id UUID NOT NULL REFERENCES product_units(id) ON DELETE RESTRICT,
    subtotal_amount NUMERIC(15,2) NOT NULL CHECK (subtotal_amount > 0),
    platform_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (platform_fee_amount >= 0),
    seller_net_amount NUMERIC(15,2) NOT NULL CHECK (seller_net_amount >= 0),
    fulfillment_status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING', -- 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'DISPUTED', 'CANCELLED'
    escrow_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'HELD', 'SETTLED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED'
    tracking_number VARCHAR(100),
    courier_name VARCHAR(100),
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fulfillment_amounts CHECK (subtotal_amount = platform_fee_amount + seller_net_amount)
);

CREATE INDEX idx_fulfillment_orders_seller ON fulfillment_orders(seller_id);
CREATE INDEX idx_fulfillment_orders_unit ON fulfillment_orders(unit_id);
