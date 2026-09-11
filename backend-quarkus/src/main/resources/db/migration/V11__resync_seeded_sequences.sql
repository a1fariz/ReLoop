-- V8 seeded sellers/users with explicit ids without advancing the serial
-- sequences, so the first runtime INSERT (e.g. a new seller registering their
-- store) collided with sellers_pkey. Re-sync every seeded sequence to MAX(id).
SELECT setval('sellers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM sellers));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
