-- Marketplace browse query (findByStatus('ACTIVE')) did a full table scan:
-- the only existing index on listings is the partial unique index on unit_id.
CREATE INDEX idx_listings_status ON listings(status);
