# inventory-service

- **Port**: 8082
- **Database**: `inventorydb` (V1 schema, `V5__inventory_item_timestamps.sql` adds `created_at`/`updated_at`)
- **Role in the system**: owns stock. It is the only service allowed to reserve, deduct or release units.

## Endpoints

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/v1/inventory` | `ROLE_ADMIN` / `ROLE_INTERNAL` |
| GET | `/api/v1/inventory?page=&size=` | authenticated |
| GET | `/api/v1/inventory/{skuCode}` | authenticated |
| PUT | `/api/v1/inventory/{skuCode}` | `ROLE_ADMIN` / `ROLE_INTERNAL` |
| DELETE | `/api/v1/inventory/{skuCode}` | `ROLE_ADMIN` / `ROLE_INTERNAL` |
| POST | `/api/v1/inventory/reservations` | `ROLE_ADMIN` / `ROLE_INTERNAL` |
| DELETE | `/api/v1/inventory/reservations/{orderNumber}` | `ROLE_ADMIN` / `ROLE_INTERNAL` |
| POST | `/api/v1/inventory/reservations/{orderNumber}/confirm` | `ROLE_ADMIN` / `ROLE_INTERNAL` |

## Events

| Direction | Topic | Effect |
| --- | --- | --- |
| consumes | `order-created` | reserves the requested units atomically |
| consumes | `order-confirmed` | turns the reservation into a real deduction |
| consumes | `order-cancelled` | releases the reservation and returns the units |
| consumes | `product-created` / `product-updated` / `product-deleted` | mirrors the catalogue |
| produces | `inventory-reserved` / `inventory-rejected` | the order saga's answer, with the rejection reason |
| produces | `inventory-events` | stock changes for the catalogue mirror |

## Implementation notes

- Reservations are keyed by order number and carry a status (`CONFIRMED`, `RELEASED`), which makes redelivered events harmless.
- Rejection is a normal outcome, not an exception: it publishes `inventory-rejected` and lets order-service cancel the order, which then drives the release.
- Reads require a token so stock levels are not scraped anonymously.

## Configuration

`DB_NAME=inventorydb`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `KAFKA_URL`, `REDIS_HOST`, `REDIS_PORT`, `CORS_ALLOWED_ORIGINS`.

## Tests

26 unit tests.
