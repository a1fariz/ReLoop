-- V18: Seed mock data for the new modules (cart, payments, ownership, returns,
-- notifications, KYC, refurbishment). References the V8 seed rows.

-- 1. KYC: customer verified, seller submitted (admin can review in UI)
UPDATE users SET kyc_status = 'VERIFIED', kyc_document_type = 'KTP', kyc_document_reference = 'KTP-327301010190001',
                 national_id = '3273010101900001', date_of_birth = '1990-01-01', address = '{"street": "Jl. Merdeka 12", "city": "Bandung", "postalCode": "40115"}',
                 kyc_submitted_at = NOW() - INTERVAL '30 days', kyc_verified_at = NOW() - INTERVAL '29 days'
WHERE id = 1;

INSERT INTO user_kyc_logs (id, user_id, previous_status, new_status, reviewed_by, notes, created_at) VALUES
    ('12121212-1212-1212-1212-121212121201', 1, 'PENDING', 'SUBMITTED', NULL, NULL, NOW() - INTERVAL '30 days'),
    ('12121212-1212-1212-1212-121212121202', 1, 'SUBMITTED', 'VERIFIED', 4, 'Identity documents verified against Dukcapil', NOW() - INTERVAL '29 days'),
    ('12121212-1212-1212-1212-121212121203', 2, 'PENDING', 'SUBMITTED', NULL, NULL, NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;

UPDATE users SET kyc_status = 'SUBMITTED', kyc_document_type = 'KTP', kyc_document_reference = 'KTP-327301550887002',
                 national_id = '3273015508870002', date_of_birth = '1988-07-05', address = '{"street": "Jl. Asia Afrika 88", "city": "Bandung", "postalCode": "40111"}',
                 kyc_submitted_at = NOW() - INTERVAL '2 days'
WHERE id = 2;

-- 2. Ownership provenance: the three seeded units were platform-acquired trade-ins
INSERT INTO ownership_transfers (id, unit_id, from_owner_id, to_owner_id, transfer_type, reference_type, reference_id, transferred_at) VALUES
    ('15151515-1515-1515-1515-151515151501', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NULL, 2, 'TRADE_IN', 'TRADE_IN_REQUEST', '54545454-5454-5454-5454-545454545451', NOW() - INTERVAL '20 days'),
    ('15151515-1515-1515-1515-151515151502', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NULL, 2, 'TRADE_IN', 'TRADE_IN_REQUEST', '54545454-5454-5454-5454-545454545452', NOW() - INTERVAL '18 days'),
    ('15151515-1515-1515-1515-151515151503', 'cccccccc-cccc-cccc-cccc-cccccccccccc', NULL, 2, 'TRADE_IN', 'TRADE_IN_REQUEST', '54545454-5454-5454-5454-545454545453', NOW() - INTERVAL '15 days')
ON CONFLICT (id) DO NOTHING;

-- 3. Notifications: buyer sees escrow + order events from the seeded order history
INSERT INTO notifications (id, recipient_id, title, body, category, reference_type, reference_id, is_read, created_at) VALUES
    ('17171717-1717-1717-1717-171717171701', 1, 'Checkout lease granted', 'Your 15-minute anti-hoarding reservation was granted for iPhone 15 Pro.', 'ORDER', 'LISTING', 'dddddddd-dddd-dddd-dddd-dddddddddddd', true, NOW() - INTERVAL '5 days'),
    ('17171717-1717-1717-1717-171717171702', 1, 'Escrow funded', 'Rp 17.500.000 is now held in double-entry escrow for order ORD-SEED-0001.', 'ESCROW', 'MASTER_ORDER', '61616161-6161-6161-6161-616161616161', true, NOW() - INTERVAL '5 days'),
    ('17171717-1717-1717-1717-171717171703', 1, 'KYC verified', 'Your identity verification passed. You can now request returns and payouts.', 'SYSTEM', 'USER', NULL, false, NOW() - INTERVAL '29 days'),
    ('17171717-1717-1717-1717-171717171704', 2, 'New sale pending shipment', 'A buyer reserved your MacBook Air M2 listing. Ship within 2 business days.', 'ORDER', 'FULFILLMENT_ORDER', '62626262-6262-6262-6262-626262626262', false, NOW() - INTERVAL '3 days'),
    ('17171717-1717-1717-1717-171717171705', 3, 'Repair bench ticket', 'New repair ticket assigned: ThinkPad X1 charging port replacement.', 'RETURN', 'REPAIR_TICKET', '76767676-7676-7676-7676-767676767676', false, NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;

-- 4. Repair ticket (refurbishment): ThinkPad charging port, mid-flow at IN_PROGRESS
INSERT INTO repair_tickets (id, unit_id, technician_id, issue_description, replaced_components, parts_cost, status, created_at, updated_at) VALUES
    ('76767676-7676-7676-7676-767676767676', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 3,
     'Charging port intermittent; USB-C pins corroded. Battery drains under load.',
     '[]', 450000.00, 'IN_PROGRESS', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- 5. Cart snapshot for the buyer (no inventory lock — read-only snapshot)
INSERT INTO cart_items (id, user_id, listing_id, quantity) VALUES
    ('13131313-1313-1313-1313-131313131301', 1, 'dddddddd-dddd-dddd-dddd-dddddddddddd', 1),
    ('13131313-1313-1313-1313-131313131302', 1, 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 1)
ON CONFLICT (id) DO NOTHING;
