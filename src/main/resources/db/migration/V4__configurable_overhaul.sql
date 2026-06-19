-- ============================================================
-- V4: DB-Configurable Overhaul
-- ============================================================

-- Library Config Table (key-value store for admin-configurable settings)
CREATE TABLE library_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description  TEXT,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed default config: library name prefix for member IDs
INSERT INTO library_config (config_key, config_value, description) VALUES
('library_name',       'BR Ambedkar Study Library', 'Display name of the library'),
('member_id_prefix',   'BRA',                       'Prefix used for auto-generated member IDs (e.g. BRA-001)'),
('member_id_counter',  '0',                         'Last used member ID sequence number');

-- ============================================================
-- Users Table Additions
-- ============================================================

-- Unique member ID (e.g. BRA-001) — used for login
ALTER TABLE users ADD COLUMN member_id VARCHAR(20) UNIQUE;

-- Address (optional)
ALTER TABLE users ADD COLUMN address TEXT;

-- Account activation (admin can deactivate/reactivate)
ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Last login timestamp
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

-- Assigned seat (FK, nullable — set at registration)
ALTER TABLE users ADD COLUMN assigned_seat_id BIGINT REFERENCES seats(id) ON DELETE SET NULL;

-- Make email nullable (students identified by member_id; only admin has real email)
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Generate member_id for the existing admin (special fixed ID)
UPDATE users SET member_id = 'ADMIN-001' WHERE role = 'ADMIN';

-- ============================================================
-- Membership Plans Table Additions
-- ============================================================

-- Link a plan to a specific shift (nullable — full-day plans may span all shifts)
ALTER TABLE membership_plans ADD COLUMN shift_id BIGINT REFERENCES shifts(id) ON DELETE SET NULL;

-- Soft-delete / disable plans without losing historical data
ALTER TABLE membership_plans ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================================
-- Shifts Table Additions
-- ============================================================

-- Soft-delete shifts without losing booking history
ALTER TABLE shifts ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
