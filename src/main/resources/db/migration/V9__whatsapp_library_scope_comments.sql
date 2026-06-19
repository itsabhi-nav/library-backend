-- Library WhatsApp: single-tenant scope (org_id column always = 'library')
-- No env var needed — code uses WhatsAppProperties.SCOPE_KEY = 'library'

COMMENT ON TABLE whatsapp_templates IS 'Meta-approved templates. Library rows use org_id = library';
COMMENT ON TABLE whatsapp_messages IS 'Permanent log of every send attempt (audit + rate limits). Not the queue.';
COMMENT ON TABLE whatsapp_message_queue IS 'Pending/retry work queue. Rows move to sent/failed here; successful sends also logged in whatsapp_messages.';
COMMENT ON TABLE whatsapp_webhook_events IS 'Optional: delivery/read status from Meta webhooks (future).';
