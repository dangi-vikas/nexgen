# order-service

- **Port**: 8084
- **Database**: `orderdb` (V1 schema, `V3` outbox, `V4` processed events, `V5` outbox payload size, `V6__orders_status_paid.sql`)
- **Role in the system**: the saga driver. It owns the order lifecycle and reacts to inventory and payment answers.

## Endpoints

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/v1/orders` | authenticated; body carries `orderItems` and an `idempotencyKey` |
| GET | `/api/v1/orders/{orderNumber}` | owner, `ROLE_ADMIN` or `ROLE_INTERNAL` |
| GET | `/api/v1/orders/user/{userId}` | owner, `ROLE_ADMIN` or `ROLE_INTERNAL` |
| PUT | `/api/v1/orders/{orderNumber}/cancel` | owner, `ROLE_ADMIN` or `ROLE_INTERNAL` |
| PUT | `/api/v1/orders/{orderNumber}/status` | `ROLE_ADMIN` or `ROLE_INTERNAL` |
| GET | `/api/v1/orders/{orderNumber}/history` | owner, `ROLE_ADMIN` or `ROLE_INTERNAL` |

`userId` is always derived from the access token, never from the request body.

## Status machine

`CREATED` (awaiting inventory) → `CONFIRMED` (stock reserved) → `PAID` (payment authorised) → `DELIVERED`; `CANCELLED` is reachable from `CREATED`, `PENDING`, `CONFIRMED` and `PAID`. `OrderStatus.canTransitionTo` enforces the graph in code and a database CHECK constraint (`chk_orders_status`) enforces the same set of values — both must list `PAID`, or the authorisation step fails with a constraint violation.

## Events

| Direction | Topic | Effect |
| --- | --- | --- |
| produces | `order-created` | starts the saga (items included) |
| produces | `order-confirmed` | triggers deduction, payment, cart clear and the confirmation e-mail |
| produces | `order-cancelled` | releases stock and refunds |
| produces | `order-status-updated` | customer notifications on later transitions |
| consumes | `inventory-reserved` / `inventory-rejected` | `CREATED → CONFIRMED` or `CREATED → CANCELLED` |
| consumes | `payment-authorized` | `CONFIRMED → PAID` |
| consumes | `payment-failed` | `CONFIRMED → CANCELLED` |
| consumes | `payment-refunded` | recorded for audit, no status change |

## Implementation notes

- Pricing is server-side: creating an order looks the price up from product-service. That price lookup is the only synchronous dependency left.
- `OutboxPublisher` awaits the Kafka send result before marking a row `PUBLISHED`; failed sends retry with the row still `PENDING` and move to `FAILED` after `outbox.publish.max-retries`.
- Consumers record `eventId` in `processed_events`, so redelivery is a no-op.
- Status changes write an `order_status_history` row and publish an event in the same transaction.

## Configuration

`DB_NAME=orderdb`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `KAFKA_URL`, `REDIS_HOST`, `REDIS_PORT`, `PRODUCT_SERVICE_URL`, `CORS_ALLOWED_ORIGINS`.

## Tests

33 unit tests.
