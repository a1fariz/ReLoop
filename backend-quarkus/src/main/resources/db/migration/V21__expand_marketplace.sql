-- V21: Expand the marketplace into a realistic multi-seller catalog.
-- Adds a second verified seller, more product models across all four
-- categories, ~15 fresh ACTIVE listings (5 existing + 15 = 20 total), and a
-- matching set of inspection records.

-- 1. Second verified seller (user 5 + sellers profile 2)
INSERT INTO users (id, email, password_hash, full_name, phone_number, role, is_verified, is_locked)
VALUES (5, 'seller2@reloop.com',
        '$2a$12$IQMBFvrOYZdzZG3aBHaxYevO3AsePugYcTR4WXStSJlPGrZCc4rma',
        'GadgetHub', '+6281100000002', 'SELLER', true, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sellers (id, user_id, store_name, store_slug, description, reputation_score, return_rate, dispute_rate, response_rate, completed_orders, active_disputes, kyc_status) VALUES
    (2, 5, 'GadgetHub Tekno', 'gadgethub-tekno', 'Curated premium devices with verified inspection reports.', 93.00, 0.010, 0.010, 0.980, 86, 0, 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

-- 2. Product models to broaden the catalog
INSERT INTO product_models (id, category_id, brand, model_name, slug, msrp, annual_depreciation_rate, release_date, specifications) VALUES
    ('66661111-6666-6666-6666-666666661111', 1, 'Apple', 'iPhone 15 128GB', 'iphone-15-128gb', 15999000.00, 0.160, '2023-09-22', '{"chip": "A16 Bionic", "ram": "6GB", "storage": "128GB", "color": "Blue"}'),
    ('66661112-6666-6666-6666-666666661112', 1, 'Apple', 'iPhone 14 Pro 128GB', 'iphone-14-pro-128gb', 18499000.00, 0.170, '2022-09-16', '{"chip": "A16 Bionic", "ram": "6GB", "storage": "128GB", "color": "Deep Purple"}'),
    ('66661113-6666-6666-6666-666666661113', 1, 'Google', 'Pixel 8 128GB', 'pixel-8-128gb', 11999000.00, 0.200, '2023-10-12', '{"chip": "Tensor G3", "ram": "8GB", "storage": "128GB", "color": "Obsidian"}'),
    ('66661114-6666-6666-6666-666666661114', 1, 'Samsung', 'Galaxy S23 128GB', 'galaxy-s23-128gb', 12999000.00, 0.180, '2023-02-17', '{"chip": "Snapdragon 8 Gen 2", "ram": "8GB", "storage": "128GB", "color": "Phantom Black"}'),
    ('66662221-6666-6666-6666-666666662221', 2, 'Apple', 'MacBook Pro M3 14in 16GB 512GB', 'macbook-pro-m3-14-512gb', 29999000.00, 0.130, '2023-11-07', '{"chip": "Apple M3", "ram": "16GB", "storage": "512GB", "color": "Space Black"}'),
    ('66662222-6666-6666-6666-666666662222', 2, 'ASUS', 'ROG Zephyrus G14 16GB 1TB', 'rog-zephyrus-g14', 24999000.00, 0.190, '2023-04-20', '{"cpu": "Ryzen 9 7940HS", "gpu": "RTX 4060", "ram": "16GB", "storage": "1TB"}'),
    ('66662223-6666-6666-6666-666666662223', 2, 'HP', 'Spectre x360 16GB 1TB', 'hp-spectre-x360', 21999000.00, 0.200, '2023-06-01', '{"cpu": "Core i7-1360P", "ram": "16GB", "storage": "1TB", "color": "Poseidon Blue"}'),
    ('66663331-6666-6666-6666-666666663331', 3, 'Apple', 'iPad Air M2 11in 128GB', 'ipad-air-m2-128gb', 11599000.00, 0.150, '2024-05-15', '{"chip": "Apple M2", "ram": "8GB", "storage": "128GB", "color": "Blue"}'),
    ('66664441-6666-6666-6666-666666664441', 4, 'Apple', 'AirPods Pro 2nd Gen USB-C', 'airpods-pro-2-usbc', 3999000.00, 0.120, '2023-09-22', '{"chip": "H2", "charging": "USB-C MagSafe", "color": "White"}'),
    ('66664442-6666-6666-6666-666666664442', 4, 'Sony', 'WH-1000XM5 Wireless', 'sony-wh-1000xm5', 5499000.00, 0.150, '2022-05-20', '{"driver": "30mm", "noiseCancelling": "8-mic", "color": "Silver"}')
ON CONFLICT (id) DO NOTHING;

-- 3. Fresh units (seller 1 owns 10, seller 2 owns 5)
INSERT INTO product_units (id, product_model_id, serial_number, current_owner_id, current_custody, status, grade, battery_health_percentage, cost_price, selling_price, version) VALUES
    ('6666a001-6666-6666-6666-66666aa00001', '66661111-6666-6666-6666-666666661111', 'F2LZ90K8MD9P', 2, 'SELLER', 'LISTED', 'A+', 96, 12500000.00, 14200000.00, 0),
    ('6666a002-6666-6666-6666-66666aa00002', '66661112-6666-6666-6666-666666661112', 'DW18T9KXY3M', 2, 'SELLER', 'LISTED', 'A', 91, 11800000.00, 13200000.00, 0),
    ('6666a003-6666-6666-6666-66666aa00003', '66661113-6666-6666-6666-666666661113', 'PXL8OB7X2Q', 2, 'SELLER', 'LISTED', 'A', 94, 9300000.00, 10400000.00, 0),
    ('6666a004-6666-6666-6666-66666aa00004', '66661114-6666-6666-6666-666666661114', 'S23BBK4X1R', 2, 'SELLER', 'LISTED', 'B+', 87, 7600000.00, 8500000.00, 0),
    ('6666a005-6666-6666-6666-66666aa00005', '66662221-6666-6666-6666-666666662221', 'C02CT0M3MD6X', 2, 'SELLER', 'LISTED', 'A+', 98, 23800000.00, 25200000.00, 0),
    ('6666a006-6666-6666-6666-66666aa00006', '66662222-6666-6666-6666-666666662222', 'ZEPH14GJ8Q', 2, 'SELLER', 'LISTED', 'A', 95, 17200000.00, 18900000.00, 0),
    ('6666a007-6666-6666-6666-66666aa00007', '66662223-6666-6666-6666-666666662223', 'SPECTRE9X2M', 2, 'SELLER', 'LISTED', 'B+', 90, 11800000.00, 12800000.00, 0),
    ('6666a008-6666-6666-6666-66666aa00008', '66663331-6666-6666-6666-666666663331', 'IPADM2BL7K', 2, 'SELLER', 'LISTED', 'A', 96, 9800000.00, 11000000.00, 0),
    ('6666a009-6666-6666-6666-66666aa00009', '66664441-6666-6666-6666-666666664441', 'APP2USBC5J', 2, 'SELLER', 'LISTED', 'A', 100, 2900000.00, 3200000.00, 0),
    ('6666a010-6666-6666-6666-66666aa00010', '66664442-6666-6666-6666-666666664442', 'SONYXM5SLV', 2, 'SELLER', 'LISTED', 'A', 100, 2600000.00, 2950000.00, 0),
    ('6666a101-6666-6666-6666-66666aa00101', '11111111-1111-1111-1111-111111111111', 'F2LZ90K8MD6O', 5, 'SELLER', 'LISTED', 'A+', 97, 15800000.00, 17000000.00, 0),
    ('6666a102-6666-6666-6666-66666aa00102', '66663331-6666-6666-6666-666666663331', 'IPADM2BL8L', 5, 'SELLER', 'LISTED', 'A', 95, 9750000.00, 10800000.00, 0),
    ('6666a103-6666-6666-6666-66666aa00103', '66662221-6666-6666-6666-666666662221', 'C02CT0M3MD7Y', 5, 'SELLER', 'LISTED', 'A', 94, 22000000.00, 23800000.00, 0),
    ('6666a104-6666-6666-6666-66666aa00104', '66664441-6666-6666-6666-666666664441', 'APP2USBC6K', 5, 'SELLER', 'LISTED', 'A', 100, 2700000.00, 3050000.00, 0),
    ('6666a105-6666-6666-6666-66666aa00105', '66662223-6666-6666-6666-666666662223', 'SPECTRE9X3N', 5, 'SELLER', 'LISTED', 'B+', 91, 11700000.00, 12400000.00, 0)
ON CONFLICT (id) DO NOTHING;

-- 4. Active listings for all fresh units (seller profile 1 owns first 10, seller profile 2 owns last 5)
INSERT INTO listings (id, unit_id, seller_id, title, description, asking_price, status, grade_snapshot, images) VALUES
    ('6666b001-6666-6666-6666-66666bb00001', '6666a001-6666-6666-6666-66666aa00001', 1, 'iPhone 15 128GB Blue (Grade A+ Pristine)', '96% battery health, full accessories, screen protected since day one.', 14200000.00, 'ACTIVE', 'A+', '["https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800"]'),
    ('6666b002-6666-6666-6666-66666bb00002', '6666a002-6666-6666-6666-66666aa00002', 1, 'iPhone 14 Pro 128GB Deep Purple (Grade A)', '91% battery health, all cameras verified, minor frame hairline.', 13200000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1591337676887-a217a6970a8a?w=800"]'),
    ('6666b003-6666-6666-6666-66666bb00003', '6666a003-6666-6666-6666-66666aa00003', 1, 'Google Pixel 8 128GB Obsidian (Grade A)', '94% battery health, clean Android experience, charger included.', 10400000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1622979135225-d2ba269cf1ac?w=800"]'),
    ('6666b004-6666-6666-6666-66666bb00004', '6666a004-6666-6666-6666-66666aa00004', 1, 'Samsung Galaxy S23 128GB Phantom Black (Grade B+)', '87% battery health, light glass scratches. Best value compact flagship.', 8500000.00, 'ACTIVE', 'B+', '["https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800"]'),
    ('6666b005-6666-6666-6666-66666bb00005', '6666a005-6666-6666-6666-66666aa00005', 1, 'MacBook Pro M3 14in 16GB 512GB Space Black (Grade A+)', '98% battery health, 14 charge cycles. Benchmark-verified GPU.', 25200000.00, 'ACTIVE', 'A+', '["https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800"]'),
    ('6666b006-6666-6666-6666-66666bb00006', '6666a006-6666-6666-6666-66666aa00006', 1, 'ASUS ROG Zephyrus G14 RTX 4060 (Grade A)', '95% battery health, gaming laptop in excellent condition with charger.', 18900000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800"]'),
    ('6666b007-6666-6666-6666-66666bb00007', '6666a007-6666-6666-6666-66666aa00007', 1, 'HP Spectre x360 16GB 1TB Poseidon Blue (Grade B+)', '90% battery health, minor lid paint wear, keyboard and display flawless.', 12800000.00, 'ACTIVE', 'B+', '["https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800"]'),
    ('6666b008-6666-6666-6666-66666bb00008', '6666a008-6666-6666-6666-66666aa00008', 1, 'iPad Air M2 11in 128GB Blue (Grade A)', '96% battery health, Apple Pencil tip wear minimal, no screen marks.', 11000000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800"]'),
    ('6666b009-6666-6666-6666-66666bb00009', '6666a009-6666-6666-6666-66666aa00009', 1, 'AirPods Pro 2nd Gen USB-C (Grade A)', 'Fully functional ANC, verified microphone grid, replacement tips added.', 3200000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800"]'),
    ('6666b010-6666-6666-6666-66666bb00010', '6666a010-6666-6666-6666-66666aa00010', 1, 'Sony WH-1000XM5 Silver (Grade A)', 'Excellent noise cancelling, pads clean, cable and case included.', 2950000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800"]'),
    ('6666b101-6666-6666-6666-66666bb00101', '6666a101-6666-6666-6666-66666aa00101', 2, 'iPhone 15 Pro 256GB Natural Titanium (Grade A+) — GadgetHub', '97% battery health, boxed with 128 charging cycles. Fully verified by our lab.', 17000000.00, 'ACTIVE', 'A+', '["https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800"]'),
    ('6666b102-6666-6666-6666-66666bb00102', '6666a102-6666-6666-6666-66666aa00102', 2, 'iPad Air M2 11in 128GB Blue — GadgetHub', '95% battery health, digital pencil across all apps, scratch-free glass.', 10800000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800"]'),
    ('6666b103-6666-6666-6666-66666bb00103', '6666a103-6666-6666-6666-66666aa00103', 2, 'MacBook Pro M3 14in 16GB 512GB — GadgetHub', '94% battery health, International warranty until late 2026, boxed.', 23800000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800"]'),
    ('6666b104-6666-6666-6666-66666bb00104', '6666a104-6666-6666-6666-66666aa00104', 2, 'AirPods Pro 2nd Gen USB-C — GadgetHub', 'A-grade pair, ANC verified, extra silicon tips included.', 3050000.00, 'ACTIVE', 'A', '["https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800"]'),
    ('6666b105-6666-6666-6666-66666bb00105', '6666a105-6666-6666-6666-66666aa00105', 2, 'HP Spectre x360 16GB 1TB — GadgetHub', '91% battery health, light wear on palm rest, display and hinges solid.', 12400000.00, 'ACTIVE', 'B+', '["https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800"]')
ON CONFLICT (id) DO NOTHING;

-- 5. Inspection records for representative stock
INSERT INTO technical_inspections (id, unit_id, technician_id, physical_score, hardware_score, software_score, final_calculated_grade, estimated_repair_cost, technician_notes, checklist_results) VALUES
    ('6666c001-6666-6666-6666-66666cc00001', '6666a001-6666-6666-6666-66666aa00001', 3, 98, 96, 100, 'A+', 0.00, 'Fresh iPhone 15 unit. Display, battery, and biometrics fully pass.', '{"display": "PASS", "battery": "PASS", "speakers": "PASS", "cameras": "PASS", "biometrics": "PASS"}'),
    ('6666c002-6666-6666-6666-66666cc00002', '6666a005-6666-6666-6666-66666aa00005', 3, 99, 98, 100, 'A+', 0.00, 'MacBook Pro M3 with 14 cycles; keyboard, trackpad, and Thunderbolt pass.', '{"display": "PASS", "battery": "PASS", "keyboard": "PASS", "ports": "PASS"}'),
    ('6666c003-6666-6666-6666-66666cc00003', '6666a008-6666-6666-6666-66666aa00008', 3, 94, 95, 96, 'A', 0.00, 'iPad Air M2. Touchscreen and pencil pairing verified.', '{"display": "PASS", "battery": "PASS", "ports": "PASS", "cameras": "PASS"}'),
    ('6666c004-6666-6666-6666-66666cc00004', '6666a101-6666-6666-6666-66666aa00101', 3, 98, 97, 100, 'A+', 0.00, 'GadgetHub iPhone 15 Pro. Full diagnostic including Face ID.', '{"display": "PASS", "battery": "PASS", "speakers": "PASS", "cameras": "PASS", "biometrics": "PASS"}'),
    ('6666c005-6666-6666-6666-66666cc00005', '6666a103-6666-6666-6666-66666aa00103', 3, 93, 95, 97, 'A', 0.00, 'MacBook Pro M3 unit. Thermals and GPU stress test verified.', '{"display": "PASS", "battery": "PASS", "keyboard": "PASS", "ports": "PASS"}')
ON CONFLICT (id) DO NOTHING;

-- 6. Keep seeded identity sequences aligned
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('sellers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM sellers));