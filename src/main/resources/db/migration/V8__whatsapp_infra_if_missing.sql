-- WhatsApp infrastructure tables (safe on shared DB — no-op if school-backend already created them)

CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id SERIAL PRIMARY KEY,
    message_id VARCHAR(100) UNIQUE NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_language VARCHAR(10) NOT NULL DEFAULT 'hi',
    recipient_phone VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(100),
    message_status VARCHAR(20) DEFAULT 'sent' CHECK (message_status IN ('sent', 'delivered', 'read', 'failed')),
    message_type VARCHAR(20) DEFAULT 'template' CHECK (message_type IN ('template', 'text', 'media')),
    variables JSONB,
    student_id INTEGER,
    org_id VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP DEFAULT NOW(),
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS whatsapp_templates (
    id SERIAL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    template_language VARCHAR(10) NOT NULL DEFAULT 'hi',
    template_status VARCHAR(20) DEFAULT 'pending' CHECK (template_status IN ('pending', 'approved', 'rejected', 'disabled')),
    template_category VARCHAR(50) NOT NULL,
    template_content TEXT NOT NULL,
    header_type VARCHAR(20),
    header_content TEXT,
    footer_text TEXT,
    buttons JSONB,
    variables JSONB,
    org_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(template_name, template_language, org_id)
);

CREATE TABLE IF NOT EXISTS whatsapp_webhook_events (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    message_id VARCHAR(100),
    phone_number VARCHAR(20),
    status VARCHAR(20),
    timestamp TIMESTAMP NOT NULL,
    event_data JSONB,
    org_id VARCHAR(50) NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS whatsapp_message_queue (
    id SERIAL PRIMARY KEY,
    org_id VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_language VARCHAR(10) NOT NULL DEFAULT 'hi',
    phone_number VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(255),
    recipient_id INTEGER,
    variables JSONB NOT NULL DEFAULT '{}',
    message_type VARCHAR(50) NOT NULL DEFAULT 'template',
    priority INTEGER DEFAULT 5,
    scheduled_for TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    max_retries INTEGER DEFAULT 3,
    current_retries INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    batch_id VARCHAR(100),
    metadata JSONB DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_recipient ON whatsapp_messages(recipient_phone);
CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_status ON whatsapp_messages(message_status);
CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_student ON whatsapp_messages(student_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_org ON whatsapp_messages(org_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_sent_at ON whatsapp_messages(sent_at);

CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_org ON whatsapp_templates(org_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_status ON whatsapp_templates(template_status);

CREATE INDEX IF NOT EXISTS idx_whatsapp_webhook_events_processed ON whatsapp_webhook_events(processed);
CREATE INDEX IF NOT EXISTS idx_whatsapp_webhook_events_org ON whatsapp_webhook_events(org_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_webhook_events_timestamp ON whatsapp_webhook_events(timestamp);

CREATE INDEX IF NOT EXISTS idx_whatsapp_queue_org_scheduled ON whatsapp_message_queue(org_id, scheduled_for);
CREATE INDEX IF NOT EXISTS idx_whatsapp_queue_status ON whatsapp_message_queue(status);
CREATE INDEX IF NOT EXISTS idx_whatsapp_queue_batch ON whatsapp_message_queue(batch_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_queue_priority ON whatsapp_message_queue(priority);
