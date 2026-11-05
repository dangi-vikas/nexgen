# product-service

- **Port**: 8083
- **Database**: `productdb`
- **Role in the system**: the catalogue and the authority on price. Orders and carts never trust a client-supplied price.

## Endpoints

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/v1/products` | `ROLE_ADMIN` |
| GET | `/api/v1/products?page=&size=` | public |
| GET | `/api/v1/products/{skuCode}` | public |
| PUT | `/api/v1/products/{skuCode}` | `ROLE_ADMIN` |
| DELETE | `/api/v1/products/{skuCode}` | `ROLE_ADMIN` |

Reads are public because the storefront has to work before sign-in; writes are admin-only.

## Events

- **Publishes** `product-created`, `product-updated`, `product-deleted` from the outbox. Cart-service reacts to `product-deleted` by dropping the item from every cart; inventory-service mirrors catalogue changes.
- **Consumes** `inventory-events` to keep the displayed quantity aligned with the stock ledger.

## Implementation notes

- The entity maps the `products` table explicitly (`@Table(name = "products")`); without it Hibernate looks for `product` and the service fails at startup.
- Redis caching covers get/update/delete paths.
- Price changes do not retroactively alter placed orders: the order snapshot stores the priced items at creation time.

## Configuration

`DB_NAME=productdb`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `KAFKA_URL`, `REDIS_HOST`, `REDIS_PORT`, `CORS_ALLOWED_ORIGINS`.

## Tests

26 unit tests.
