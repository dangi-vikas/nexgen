# payment-service

- **Port**: 8085 (Actuator only — there is no REST API)
- **Database**: `paymentdb`
- **Role in the system**: charges the order total when the saga reaches `order-confirmed`, and refunds when a paid order is cancelled.

## Interface

Pure Kafka. The service deliberately exposes no HTTP endpoint for payments: an order's amount must come from the order event, never from a caller.

| Direction | Topic | Effect |
| --- | --- | --- |
| consumes | `order-confirmed` | charges `totalAmount` from the event; writes a payment row |
| consumes | `order-cancelled` | refunds an `AUTHORIZED` payment |
| produces | `payment-authorized` / `payment-failed` | the answer order-service acts on |
| produces | `payment-refunded` | audit trail after a refund |

## PaymentGateway port

`PaymentGateway` is the boundary a real provider would implement. The default `SimulatedPaymentGateway`:

- charges nothing and needs no credentials;
- returns a fabricated provider reference (`sim_...`) on success;
- **declines any total whose cents are `.99`**, which is the deterministic trigger the end-to-end suite uses for the compensation path.

Amounts ending in `.99` are the only failure mode, so the decline path is reproducible without stubbing anything.

## Implementation notes

- `payments` has a unique index on `order_number` (`uk_payments_order`) — one payment per order. `authorize` returns early if a payment already exists, and the row is written **once**, after the gateway answers; saving a `PENDING` row first and updating it afterwards re-inserts and collides with that index.
- Payment statuses: `PENDING`, `AUTHORIZED`, `FAILED`, `REFUNDED`.
- Results are published through the same transactional outbox pattern as every other service.

## Configuration

`DB_NAME=paymentdb`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_URL`, and `payments.currency` (`PAYMENTS_CURRENCY`, default `EUR`).

## Tests

5 unit tests.
