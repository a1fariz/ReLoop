-- V13: Read-only snapshot cart (no inventory lock — anti-hoarding invariant:
-- adding to a cart never reserves a unit; reservations happen at checkout initiation only).

CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity >= 1 AND quantity <= 5),
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cart_user_listing UNIQUE (user_id, listing_id)
);

CREATE INDEX idx_cart_items_user ON cart_items(user_id);