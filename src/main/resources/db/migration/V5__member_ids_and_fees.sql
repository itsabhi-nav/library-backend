-- ============================================================
-- V5: Member ID format (no hyphen) + Monthly fee billing
-- ============================================================

-- Migrate existing hyphenated member IDs → BRA001, ADMIN001
UPDATE users SET member_id = REPLACE(member_id, '-', '') WHERE member_id LIKE '%-%';

-- Additional config keys
INSERT INTO library_config (config_key, config_value, description) VALUES
('member_id_digits',     '3',  'Number of digits in auto-generated member IDs (e.g. 3 → BRA001)'),
('fee_due_day_of_month', '5',  'Day of month when generated fee invoices are due')
ON CONFLICT (config_key) DO NOTHING;

UPDATE library_config SET description = 'Prefix for new member IDs (e.g. BRA → BRA001). Only affects future registrations.'
WHERE config_key = 'member_id_prefix';

UPDATE library_config SET description = 'Last used member ID sequence number'
WHERE config_key = 'member_id_counter';

-- ============================================================
-- Fee Invoices
-- ============================================================
CREATE TABLE fee_invoices (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id  BIGINT REFERENCES subscriptions(id) ON DELETE SET NULL,
    billing_year     INT NOT NULL,
    billing_month    INT NOT NULL CHECK (billing_month BETWEEN 1 AND 12),
    amount           DECIMAL(10,2) NOT NULL,
    plan_name        VARCHAR(255),
    due_date         DATE NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount_paid      DECIMAL(10,2) NOT NULL DEFAULT 0,
    generated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, billing_year, billing_month)
);

CREATE INDEX idx_fee_invoices_status ON fee_invoices(status);
CREATE INDEX idx_fee_invoices_billing ON fee_invoices(billing_year, billing_month);

-- ============================================================
-- Fee Payments
-- ============================================================
CREATE TABLE fee_payments (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES fee_invoices(id) ON DELETE CASCADE,
    amount          DECIMAL(10,2) NOT NULL,
    payment_method  VARCHAR(20),
    paid_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    recorded_by     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    notes           TEXT
);

CREATE INDEX idx_fee_payments_invoice ON fee_payments(invoice_id);
