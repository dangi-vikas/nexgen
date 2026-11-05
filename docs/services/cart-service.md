# cart-service

- **Port**: 8086
- **Database**: `cartdb`, plus Redis for the `cart` cache
- **Role in the system**: holds the buyer's basket and starts checkout.

## Endpoints

All paths are scoped to a user id and only the authenticated subject may touch its own cart (`401` for anyone else).

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/cart/{userId}` | read the cart |
| POST | `/api/v1/cart/{userId}/add` | add a product (quantity in the body) |
| DELETE | `/api/v1/cart/{userId}/remove/{productId}/quantity/{quantity}` | remove units |
| DELETE | `/api/v1/cart/{userId}/clear` | empty the cart |
| POST | `/api/v1/cart/checkout` | create the order through order-service |

## Events

- **Publishes** `cart-checkout` once checkout has been accepted.
- **Consumes** `order-confirmed` to clear the buyer's cart, and `product-deleted` to drop that product from every cart.

The cart deliberately survives checkout: it is only cleared by `order-confirmed`, so a rejected stock reservation leaves the basket intact for the customer.

## Implementation notes

- `getCartByUser` is `@Cacheable(value = "cart", key = "#userId")`. Because of that, the Kafka clear path must evict the cache as well as delete rows — `CartReactionService` is annotated `@CacheEvict`, otherwise the cleared cart keeps being served from Redis.
- Checkout is the one synchronous call in the flow: the client needs an order number immediately, so cart-service asks order-service to create the order rather than waiting for an event round trip.
- Resilience4j (circuit breaker, rate limiter, timers) is configured for this service; it is not a system-wide guarantee.

## Configuration

`DB_NAME=cartdb`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `KAFKA_URL`, `REDIS_HOST`, `REDIS_PORT`, `ORDER_SERVICE_URL`, `CORS_ALLOWED_ORIGINS`.

## Tests

23 unit tests.
