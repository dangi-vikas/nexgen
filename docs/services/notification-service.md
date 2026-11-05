# notification-service

- **Port**: 8087 (Actuator only — no REST surface)
- **Database**: `notificationdb`
- **Role in the system**: turns domain events into customer messages and delivers them by e-mail.

## Events consumed

`cart-checkout`, `order-created`, `order-confirmed`, `order-cancelled`, `order-status-updated`, `user-registration`, `user-login`, `user-profile-updated`, `user-password-changed`.

Each message type maps to one listener method; anything without an `eventId` is ignored, and an event id that was already recorded is skipped.

## Delivery model

`notifications` is a **delivery outbox**, not a log:

| Column group | Meaning |
| --- | --- |
| `event_id` | dedupe key, one notification per event |
| `type`, `subject`, `message` | what to send |
| `recipient` | resolved from `user_contacts` at write time |
| `status` | `PENDING` → `SENT`, or `FAILED` after `notifications.delivery.max-attempts` |
| `attempts`, `last_error`, `sent_at`, `provider_message_id` | delivery bookkeeping |

`NotificationDeliveryJob` runs every `notifications.delivery.delay` (5s) and sends up to 50 pending rows per tick. A missing recipient is not a failure: the row stays `PENDING` until `user-registration` or `user-profile-updated` supplies an address. Scheduling must be enabled on the application class (`@EnableScheduling`), or the job never runs.

## Addresses

`user_contacts` maps `userId → e-mail`, populated from the user events. The recipient is captured on the notification row, so a later e-mail change does not rewrite message history.

## Local e-mail

Mailpit is part of the Compose stack: SMTP on `1025`, web UI on <http://localhost:8025>. `SMTP_HOST=localhost`, `SMTP_PORT=1025`, `SMTP_AUTH=false`, `SMTP_STARTTLS=false`, `NOTIFICATIONS_FROM=no-reply@nexgen.local`. No external provider or credentials are involved.

## Configuration

`DB_NAME=notificationdb`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_URL`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_AUTH`, `SMTP_STARTTLS`, `NOTIFICATIONS_FROM`, `NOTIFICATIONS_DELIVERY_ENABLED`, `notifications.delivery.delay`, `notifications.delivery.max-attempts`.

## Tests

3 unit tests.
