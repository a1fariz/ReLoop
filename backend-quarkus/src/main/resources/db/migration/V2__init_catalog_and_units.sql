-- V2: Product Models, Serialized Units & Custody Tracking

CREATE TABLE product_categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    icon_name VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE product_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id INT NOT NULL REFERENCES product_categories(id),
    brand VARCHAR(100) NOT NULL,
    model_name VARCHAR(150) NOT NULL,
    slug VARCHAR(200) NOT NULL UNIQUE,
    msrp NUMERIC(15,2) NOT NULL CHECK (msrp > 0),
    annual_depreciation_rate NUMERIC(4,3) NOT NULL DEFAULT 0.150 CHECK (annual_depreciation_rate >= 0 AND annual_depreciation_rate <= 1),
    release_date DATE NOT NULL,
    specifications JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_models_slug ON product_models(slug);
CREATE INDEX idx_product_models_brand ON product_models(brand);

CREATE TYPE product_unit_status AS ENUM (
    'DRAFT',
    'AVAILABLE',
    'RESERVED',
    'SOLD',
    'IN_TRANSIT',
    'DELIVERED',
    'OWNED',
    'TRADE_IN_PENDING',
    'IN_INSPECTION',
    'IN_REPAIR',
    'REFURBISHED',
    'READY_FOR_LISTING',
    'LISTED',
    'RETURN_INSPECTION',
    'RETURNED_TO_OWNER',
    'DAMAGED',
    'DISPOSED'
);

CREATE TYPE physical_custody_type AS ENUM (
    'OWNER',
    'PLATFORM_HUB',
    'SELLER',
    'LOGISTICS_3PL',
    'BUYER'
);

CREATE TABLE product_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_model_id UUID NOT NULL REFERENCES product_models(id),
    serial_number VARCHAR(100) NOT NULL,
    current_owner_id BIGINT NOT NULL REFERENCES users(id),
    current_custody physical_custody_type NOT NULL DEFAULT 'OWNER',
    status product_unit_status NOT NULL DEFAULT 'DRAFT',
    grade VARCHAR(5), -- 'A+', 'A', 'B+', 'B', 'C', 'D'
    battery_health_percentage INT CHECK (battery_health_percentage >= 0 AND battery_health_percentage <= 100),
    cost_price NUMERIC(15,2),
    selling_price NUMERIC(15,2),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_model_serial UNIQUE (product_model_id, serial_number)
);

CREATE INDEX idx_product_units_status ON product_units(status);
CREATE INDEX idx_product_units_owner ON product_units(current_owner_id);
