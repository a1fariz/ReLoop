-- V20: Restock the marketplace with fresh listable units.
-- The two seed units (iPhone, MacBook) were consumed by the checkout smoke test
-- and are SOLD; this migration adds new inventory + listings for a livelier demo.

-- 1. Additional product models (smartphones category)
INSERT INTO product_models (id, category_id, brand, model_name, slug, msrp, annual_depreciation_rate, release_date, specifications) VALUES
    ('44444444-4444-4444-4444-444444444444', 1, 'Samsung', 'Galaxy S23 Ultra 256GB', 'galaxy-s23-ultra-256gb', 18999000.00, 0.180, '2023-02-17', '{"chip": "Snapdragon 8 Gen 2", "ram": "12GB", "storage": "256GB", "color": "Cream"}'),
    ('55555555-5555-5555-5555-555555555555', 1, 'Xiaomi', 'Redmi Note 13 Pro 8GB 256GB', 'redmi-note-13-pro-256gb', 3999000.00, 0.250, '2024-01-15', '{"chip": "Helio G99", "ram": "8GB", "storage": "256GB", "color": "Midnight Black"}')
ON CONFLICT (id) DO NOTHING;

-- 2. Fresh units owned by the seeded seller (seller user id 2), listed for sale
INSERT INTO product_units (id, product_model_id, serial_number, current_owner_id, current_custody, status, grade, battery_health_percentage, cost_price, selling_price, version) VALUES
    ('6161a1a1-6161-6161-6161-616161616161', '11111111-1111-1111-1111-111111111111', 'F2LZ90K8MD7N', 2, 'SELLER', 'LISTED', 'A+', 97, 15700000.00, 17300000.00, 0),
    ('6262b2b2-6262-6262-6262-626262626262', '22222222-2222-2222-2222-222222222222', 'C02G89A3MD8U', 2, 'SELLER', 'LISTED', 'A', 92, 13800000.00, 15300000.00, 0),
    ('6363c3c3-6363-6363-6363-636363636363', '44444444-4444-4444-4444-444444444444', 'R5CX1234AB9', 2, 'SELLER', 'LISTED', 'A', 95, 13500000.00, 14900000.00, 0),
    ('6464d4d4-6464-6464-6464-646464646464', '55555555-5555-5555-5555-555555555555', 'XMI12345Z9W', 2, 'SELLER', 'LISTED', 'B+', 88, 2650000.00, 3199000.00, 0)
ON CONFLICT (id) DO NOTHING;

-- 3. Active listings for the fresh units
INSERT INTO listings (id, unit_id, seller_id, title, description, asking_price, status, grade_snapshot, images) VALUES
    ('7171a1a1-7171-7171-7171-717171717171', '6161a1a1-6161-6161-6161-616161616161', 1, 'iPhone 15 Pro 256GB Natural Titanium (Grade A+ Pristine)', 'Restock unit — 97% battery health, full box, certified inspection report attached.', 17300000.00, 'ACTIVE', 'A+', '["https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800"]'),
    ('7272b2b2-7272-7272-7272-727272727272', '6262b2b2-6262-6262-6262-626262626262', 1, 'MacBook Air M2 16GB 512GB Midnight (Grade A)', 'Restock unit — 92% battery health with original 35W dual charger. Light cosmetic wear only.', 15300000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800"]'),
    ('7373c3c3-7373-7373-7373-737373737373', '6363c3c3-6363-6363-6363-636363636363', 1, 'Samsung Galaxy S23 Ultra 256GB Cream (Grade A)', '95% battery health, S-Pen included. Screen and camera verified in mint condition.', 14900000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800"]'),
    ('7474d4d4-7474-7474-7474-747474747474', '6464d4d4-6464-6464-6464-646464646464', 1, 'Redmi Note 13 Pro 8GB 256GB Midnight (Grade B+)', 'Budget pick — 88% battery health, minor back-panel scuffs. Internals fully verified.', 3199000.00, 'ACTIVE', 'B+', '["https://images.unsplash.com/photo-1580910051074-3eb694886505?w=800"]')
ON CONFLICT (id) DO NOTHING;

-- 4. Inspection records backing the restocked grades
INSERT INTO technical_inspections (id, unit_id, technician_id, physical_score, hardware_score, software_score, final_calculated_grade, estimated_repair_cost, technician_notes, checklist_results) VALUES
    ('8181a1a1-8181-8181-8181-818181818181', '6161a1a1-6161-6161-6161-616161616161', 3, 97, 96, 100, 'A+', 0.00, 'Restock unit passed full diagnostic; display, battery, and biometrics verified.', '{"display": "PASS", "battery": "PASS", "speakers": "PASS", "cameras": "PASS", "biometrics": "PASS"}'),
    ('8282b2b2-8282-8282-8282-828282828282', '6262b2b2-6262-6262-6262-626262626262', 3, 90, 93, 95, 'A', 0.00, 'Two small cosmetic marks on chassis; internals verified at 100%.', '{"display": "PASS", "battery": "PASS", "keyboard": "PASS", "ports": "PASS"}')
ON CONFLICT (id) DO NOTHING;