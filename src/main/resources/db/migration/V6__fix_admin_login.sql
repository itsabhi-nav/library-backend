-- ============================================================
-- V6: Fix admin login after member ID format change
-- Ensures ADMIN001 + admin123 works even if V5 was skipped/partial
-- ============================================================

-- Remove hyphens from all member IDs
UPDATE users SET member_id = REPLACE(member_id, '-', '') WHERE member_id LIKE '%-%';

-- Force admin account to known-good credentials
UPDATE users
SET member_id = 'ADMIN001',
    password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    is_active = true
WHERE role = 'ADMIN' OR email = 'admin@study.com';
