-- Member date of birth + WhatsApp marketing consent (default true on registration)

ALTER TABLE users ADD COLUMN IF NOT EXISTS dob DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp_consent BOOLEAN NOT NULL DEFAULT true;

-- Existing members: opt-in assumed for library communications
UPDATE users SET whatsapp_consent = true WHERE whatsapp_consent IS NOT true;

COMMENT ON COLUMN users.dob IS 'Member date of birth (optional)';
COMMENT ON COLUMN users.whatsapp_consent IS 'Consent for WhatsApp messages; true by default at registration';
