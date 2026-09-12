-- V19: Fix seeded demo credentials — the V8 placeholder bcrypt hashes did not
-- match "SecurePass123!". Regenerated at cost 12 with spring-security-crypto 6.3.3,
-- identical to AuthService's encoder. All four demo accounts share this password.

UPDATE users
SET password_hash = '$2a$12$IQMBFvrOYZdzZG3aBHaxYevO3AsePugYcTR4WXStSJlPGrZCc4rma'
WHERE id IN (1, 2, 3, 4)
  AND email LIKE '%@reloop.com';
