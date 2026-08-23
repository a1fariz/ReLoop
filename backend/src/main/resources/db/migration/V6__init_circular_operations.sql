-- V6: Circular Operations (Trade-In, Inspection, Grading, Warranty & Disputes)

CREATE TABLE trade_in_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    product_model_id UUID NOT NULL REFERENCES product_models(id),
    declared_condition VARCHAR(30) NOT NULL, -- 'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'DAMAGED'
    declared_functionality VARCHAR(30) NOT NULL, -- 'FULLY_FUNCTIONAL', 'MINOR_ISSUES', 'MAJOR_ISSUES', 'NOT_WORKING'
    declared_battery_health INT,
    has_complete_accessories BOOLEAN NOT NULL DEFAULT FALSE,
    estimated_offer NUMERIC(15,2) NOT NULL CHECK (estimated_offer >= 0),
    final_counter_offer NUMERIC(15,2),
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', -- 'SUBMITTED', 'IN_TRANSIT', 'INSPECTING', 'COUNTER_OFFER_PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED'
    unit_id UUID REFERENCES product_units(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE technical_inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id),
    technician_id BIGINT NOT NULL REFERENCES users(id),
    trade_in_request_id UUID REFERENCES trade_in_requests(id),
    template_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    physical_score INT NOT NULL CHECK (physical_score >= 0 AND physical_score <= 100),
    hardware_score INT NOT NULL CHECK (hardware_score >= 0 AND hardware_score <= 100),
    software_score INT NOT NULL CHECK (software_score >= 0 AND software_score <= 100),
    final_calculated_grade VARCHAR(5) NOT NULL, -- 'A+', 'A', 'B+', 'B', 'C', 'D'
    estimated_repair_cost NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    technician_notes TEXT,
    checklist_results JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE repair_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id),
    technician_id BIGINT NOT NULL REFERENCES users(id),
    issue_description TEXT NOT NULL,
    replaced_components JSONB NOT NULL DEFAULT '[]',
    parts_cost NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'DIAGNOSING', 'IN_PROGRESS', 'QC_PENDING', 'COMPLETED', 'CANCELLED'
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE warranties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id),
    owner_id BIGINT NOT NULL REFERENCES users(id),
    fulfillment_order_id UUID NOT NULL REFERENCES fulfillment_orders(id),
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    policy_tier VARCHAR(50) NOT NULL DEFAULT 'STANDARD_6_MONTHS',
    is_voided BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_warranty_duration CHECK (expires_at > starts_at)
);

CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fulfillment_order_id UUID NOT NULL REFERENCES fulfillment_orders(id),
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES sellers(id),
    reason VARCHAR(100) NOT NULL,
    claim_description TEXT NOT NULL,
    evidence_images JSONB NOT NULL DEFAULT '[]',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'UNDER_REVIEW', 'EVIDENCE_COLLECTING', 'RESOLVED', 'CLOSED'
    resolution_type VARCHAR(50), -- 'FULL_REFUND', 'PARTIAL_REFUND', 'REPAIR', 'REPLACEMENT', 'RELEASE_PAYMENT'
    buyer_refund_amount NUMERIC(15,2) DEFAULT 0.00,
    seller_payout_amount NUMERIC(15,2) DEFAULT 0.00,
    resolution_notes TEXT,
    resolved_by_admin_id BIGINT REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
