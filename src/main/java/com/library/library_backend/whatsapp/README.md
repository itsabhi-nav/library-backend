# WhatsApp module (library-backend)

Single-tenant library — **no orgId concept**. The DB column `org_id` is kept for school-backend compatibility and is always set to **`library`** internally.

## Environment variables you need

| Variable | Required | Purpose |
|----------|----------|---------|
| `WHATSAPP_ENABLED` | Yes (set `true`) | Master switch |
| `WHATSAPP_ACCESS_TOKEN` | Yes | Meta Cloud API token |
| `WHATSAPP_PHONE_NUMBER_ID` | Yes | Meta phone number ID |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | For webhooks | Meta webhook verification (optional until webhooks added) |
| `WHATSAPP_API_VERSION` | No (default `v21.0`) | Graph API version |
| `WHATSAPP_BASE_URL` | No (default `https://graph.facebook.com`) | Graph API base |
| `WHATSAPP_RATE_LIMIT_PER_SECOND` | No (default `80`) | Per-second cap |
| `WHATSAPP_RATE_LIMIT_PER_DAY` | No (default `10000`) | Daily cap |
| `WHATSAPP_MONTHLY_COLLECTION_RECIPIENTS` | For admin reports | Comma-separated admin phones (e.g. `9876543210,9123456789`) |
| `WHATSAPP_QUEUE_RETENTION_DAYS` | No (default `10`) | How long to keep old queue rows |

You do **not** need `WHATSAPP_ORG_ID`.

## Database tables (4 — do not merge queue + messages)

```
whatsapp_templates     → Meta template definitions (name, language, status, content)
whatsapp_message_queue → Work queue: pending → processing → sent/failed + retries
whatsapp_messages      → Permanent send log (used for rate limits + history)
whatsapp_webhook_events→ Delivery/read events from Meta (optional, future)
```

### Why queue and messages are separate (same as school-backend)

| | `whatsapp_message_queue` | `whatsapp_messages` |
|--|--------------------------|---------------------|
| Purpose | Hold messages **waiting** to send or **retrying** | **Audit log** of what Meta accepted/rejected |
| Lifecycle | pending → sent/failed, then deleted after retention | Kept for history + rate-limit counting |
| Retries | `current_retries`, `max_retries`, midnight reset | One row per API attempt |

Merging them would break retry logic and rate-limit SQL. Keep both.

Created by Flyway: `V8__whatsapp_infra_if_missing.sql` (+ scope comments in `V9`).

## Insert your Meta template before testing

Use **org_id = `library`** (fixed, not configurable):

```sql
INSERT INTO whatsapp_templates (
  template_name, template_language, template_status, template_category,
  template_content, org_id
) VALUES (
  'your_meta_template_name', 'hi', 'approved', 'UTILITY',
  'Hello {{1}}, ...',
  'library'
);
```

Template name must match Meta exactly.

## Send a test message (from code)

```java
@Autowired WhatsAppService whatsApp;

whatsApp.sendTemplateMessage(
    "9876543210",
    "your_meta_template_name",
    "hi",
    Map.of("1", "Member Name")
);
```

No orgId parameter — scope is always `library`.

## Bulk / cron jobs (later)

```java
whatsApp.sendBulkTemplateMessages("your_template", "hi", recipients, null);
```

## Admin report phones

```java
properties.getMonthlyCollectionRecipientList(); // from WHATSAPP_MONTHLY_COLLECTION_RECIPIENTS
```

Use when you add monthly collection report cron jobs.

## Maintenance crons (IST, when enabled)

- **12:03 AM** — retry failed queue rows + drain pending queue
- **12:10 AM** — delete old sent/failed queue rows

## Ready for your test

1. Set env vars above in `.env`
2. Run backend (Flyway applies V8/V9 if not already)
3. Insert template row with `org_id = 'library'`
4. Share template name + test phone — we can wire a one-shot test send
