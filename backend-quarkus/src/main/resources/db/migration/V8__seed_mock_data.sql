-- V8: Rich Realistic Mock Data for Immediate End-to-End Testing

-- 1. Insert Categories
INSERT INTO product_categories (id, name, slug, icon_name) VALUES
    (1, 'Smartphones', 'smartphones', 'smartphone'),
    (2, 'Laptops & MacBooks', 'laptops', 'laptop'),
    (3, 'Tablets & iPads', 'tablets', 'tablet'),
    (4, 'Audio & Accessories', 'audio', 'headphones')
ON CONFLICT (id) DO NOTHING;

-- 2. Insert Users (Password: SecurePass123! - hashed via BCrypt)
-- $2a$12$e8Y7Yx4Xj2f3nB9Vd.O2xeX0N9L6y1aU7P3qW5vK8M2T4zR6sI8Gq (example BCrypt hash)
INSERT INTO users (id, email, password_hash, full_name, phone_number, role, is_verified, is_locked) VALUES
    (1, 'customer@reloop.com', '$2a$12$7kP.K.8P8ZzP4s3Yy0V2g.o9mZ2K4V6vK8M2T4zR6sI8Gqe8Y7Yx4', 'Budi Santoso', '+6281234567890', 'CUSTOMER', true, false),
    (2, 'seller@reloop.com', '$2a$12$7kP.K.8P8ZzP4s3Yy0V2g.o9mZ2K4V6vK8M2T4zR6sI8Gqe8Y7Yx4', 'Official iBox ReLoop', '+6281987654321', 'SELLER', true, false),
    (3, 'tech@reloop.com', '$2a$12$7kP.K.8P8ZzP4s3Yy0V2g.o9mZ2K4V6vK8M2T4zR6sI8Gqe8Y7Yx4', 'Agus Certified Tech', '+6281122334455', 'TECHNICIAN', true, false),
    (4, 'admin@reloop.com', '$2a$12$7kP.K.8P8ZzP4s3Yy0V2g.o9mZ2K4V6vK8M2T4zR6sI8Gqe8Y7Yx4', 'ReLoop Master Admin', '+6281000000000', 'ADMIN', true, false)
ON CONFLICT (id) DO NOTHING;

-- 3. Insert Verified Seller Profile
INSERT INTO sellers (id, user_id, store_name, store_slug, description, reputation_score, return_rate, dispute_rate, response_rate, completed_orders, active_disputes, kyc_status) VALUES
    (1, 2, 'Official iBox Certified Hub', 'ibox-certified', 'Premier certified circular retail partner with 50-point technical grading certification.', 98.50, 0.008, 0.000, 0.995, 240, 0, 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

-- 4. Insert Canonical Product Models
INSERT INTO product_models (id, category_id, brand, model_name, slug, msrp, annual_depreciation_rate, release_date, specifications) VALUES
    ('11111111-1111-1111-1111-111111111111', 1, 'Apple', 'iPhone 15 Pro 256GB', 'iphone-15-pro-256gb', 20999000.00, 0.150, '2023-09-22', '{"chip": "A17 Pro", "ram": "8GB", "storage": "256GB", "color": "Natural Titanium"}'),
    ('22222222-2222-2222-2222-222222222222', 2, 'Apple', 'MacBook Air M2 16GB 512GB', 'macbook-air-m2-512gb', 18999000.00, 0.120, '2022-07-15', '{"chip": "Apple M2", "ram": "16GB", "storage": "512GB", "color": "Midnight"}'),
    ('33333333-3333-3333-3333-333333333333', 2, 'Lenovo', 'ThinkPad X1 Carbon Gen 10', 'thinkpad-x1-carbon-gen-10', 24500000.00, 0.200, '2022-04-10', '{"cpu": "Intel Core i7-1260P", "ram": "16GB", "storage": "512GB"}')
ON CONFLICT (id) DO NOTHING;

-- 5. Insert Serialized Product Units
INSERT INTO product_units (id, product_model_id, serial_number, current_owner_id, current_custody, status, grade, battery_health_percentage, cost_price, selling_price) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'F2LZ90K8MD6M', 2, 'SELLER', 'AVAILABLE', 'A+', 98, 15000000.00, 17500000.00),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 'C02G89A3MD6T', 2, 'SELLER', 'AVAILABLE', 'A', 94, 13500000.00, 15800000.00),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '33333333-3333-3333-3333-333333333333', 'PF3A9812ZK09', 2, 'SELLER', 'AVAILABLE', 'B+', 89, 10500000.00, 12900000.00)
ON CONFLICT (id) DO NOTHING;

-- 6. Insert Active Verified Listings
INSERT INTO listings (id, unit_id, seller_id, title, description, asking_price, status, grade_snapshot, images) VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1, 'iPhone 15 Pro 256GB Natural Titanium (Grade A+ Pristine)', 'Pristine condition with 98% battery health, full box, and certified inspection report.', 17500000.00, 'ACTIVE', 'A+', '["https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800"]'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 1, 'MacBook Air M2 16GB 512GB Midnight (Grade A)', 'Barely used workstation, 94% battery health with original Apple 35W Dual Charger.', 15800000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800"]'),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 1, 'ThinkPad X1 Carbon Gen 10 16GB 512GB (Grade B+)', 'Minor cosmetic marks on bottom lid. Screen and hardware 100% flawless.', 12900000.00, 'ACTIVE', 'B+', '["https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800"]')
ON CONFLICT (id) DO NOTHING;

-- 7. Insert Technical Inspection Records
INSERT INTO technical_inspections (id, unit_id, technician_id, physical_score, hardware_score, software_score, final_calculated_grade, estimated_repair_cost, technician_notes, checklist_results) VALUES
    ('99999999-9999-9999-9999-999999999991', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 3, 98, 97, 100, 'A+', 0.00, 'Device in near-mint condition. Battery diagnostic passes with 98% health. Display true tone & face id active.', '{"display": "PASS", "battery": "PASS", "speakers": "PASS", "cameras": "PASS", "biometrics": "PASS"}')
ON CONFLICT (id) DO NOTHING;
