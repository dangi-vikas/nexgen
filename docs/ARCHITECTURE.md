# NexGen Architecture

**Status:** Baseline current-state description at commit `bf41b37`, plus Batch 1 (user-service) and Batch 2 (service authentication/authorization) working-tree deltas. Known gaps are linked to `docs/PRODUCTION_READINESS_AUDIT.md`.

## 0. Batch 1 delta (uncommitted, user-service)

- Registration no longer accepts a client `role`; `ROLE_USER` is server-assigned.
- `/register` and `/me` return `UserProfileResponse`, not the JPA entity; password serialization is blocked.
- JWT access/refresh tokens now have a type, issuer, audience, `jti`, and configurable 15-minute/7-day lifetimes.
- Refresh state is hashed in Redis, rotated atomically, and revoked on replay, logout, and password change.
- The filter accepts only access tokens; auth failures return JSON 401 (503 for Redis outages).
- Request DTOs are validated; CORS is an explicit allowlist.
- User-profile caching was removed.

## 0.1 Batch 2 delta (uncommitted, product/inventory/cart/order)

- Product, inventory, cart, and order are OAuth2 resource servers validating the user-service HS256 token (`iss`, `aud`, `exp`, `token_type=access`).
- Deny-by-default rules with explicit public product reads and public Swagger/Actuator health/info/error paths.
- JSON 401/403; stateless sessions; basic/form auth and generated user-details auto-configuration are disabled.
- Product reads are public; product writes require `ROLE_ADMIN`.
- Inventory reads require authentication; inventory writes require `ROLE_ADMIN` or `ROLE_INTERNAL`.
- Cart access is self-only based on the authenticated subject; checkout `userId` must match it.
- Order creation derives `userId` from the token; reads/cancel/history are owner-only unless admin/internal; status updates require admin/internal.
- CORS is an explicit per-service allowlist; no `@CrossOrigin` remains.
- Kong, the deployment topology, pricing/stock authority, and event reliability are unchanged.

The sections below describe the **baseline** unless explicitly marked as Batch 1 or Batch 2.

---

## 1. Context

NexGen is an e-commerce backend split into five Spring Boot services. It uses SQL Server per service, Redis for caching and token state, Kafka for events, Kong as an API gateway, and Prometheus/Grafana/Loki/Promtail for observability. There is no frontend, payment provider, notification service, or cloud deployment in the repository.

The system is best described as a **modular learning backend with service boundaries**, not a production microservices platform. The boundaries are real at the Maven/project level, but the integration behavior expected of an e-commerce system (pricing authority, stock reservation, order/payment saga, idempotency) is not implemented.

---

## 2. As-is topology

```
                       ┌─────────────────────────────┐
   Clients ──────────► │ Kong Gateway :8000          │
                       │ (scripts are broken;        │
                       │  JWT model mismatched)      │
                       └──────────────┬──────────────┘
                                      │ optional; direct ports also reachable
        ┌──────────────┬──────────────┼──────────────┬──────────────┐
        ▼              ▼              ▼              ▼              ▼
  user :8081     inventory :8082  product :8083  order :8084   cart :8086
  Spring Sec     Spring Sec       Spring Sec     Spring Sec    Spring Sec
  JWT            JWT resource     public reads,  owner/role    self-only
                 server           admin writes   checks        cart
        │              │              │              │              │
        ├──────────────┴──────────────┴──────────────┴──────────────┤
        ▼                                                           ▼
  SQL Server :1433 (5 DBs, never created)                    Redis :6379 (no auth)
        │
        ▼
  Kafka :9092 (PLAINTEXT, auto-create)
        │
        └──► Prometheus / Grafana / Loki / Promtail (partial coverage)
```

### 2.1 Service responsibilities

| Service | Responsibility | Data owned | Notes |
|---|---|---|---|
| user-service | Registration, login, JWT issue/refresh/logout, profile, password | `users` in `userdb` | Token issuer; Batch 1 adds typed tokens, hashed rotation/revocation, DTO responses, validation; Kafka producer |
| inventory-service | Stock CRUD | `inventory_items` in `inventorydb` | No reservation or atomic decrement; Batch 2 adds authenticated reads and admin/internal writes |
| product-service | Catalog CRUD | `products` in `productdb` | Batch 2 makes reads public and writes admin-only; Redis page cache; Kafka producer |
| cart-service | Cart CRUD, checkout simulation | `cart_items` in `cartdb` | Batch 2 enforces self-only access; still trusts client price and fakes payment success; no order/inventory call |
| order-service | Order CRUD, status history | `orders`, `order_items`, `order_status_history` in `orderdb` | Batch 2 derives `userId` from the token and enforces owner/privileged access; items/prices remain client-supplied; no idempotency; Kafka producer |

### 2.2 Data ownership and duplication

- Each service has its own SQL Server database and Hibernate `ddl-auto: update` schema.
- Cart and order both store a `price` snapshot, but the price is supplied by the client, not resolved from product-service.
- Order items store `skuCode`, `quantity`, and `price`; there is no foreign key or runtime validation against product/inventory.
- Status history is stored in order-service and served through `OrderService`; Batch 2 removed direct controller repository access and added owner checks.
- **Baseline:** user data was cached in Redis as the JPA entity (including the password hash) under the `users` cache. **Batch 1 removes user-profile caching entirely.**

### 2.3 API surface (current)

All paths are prefixed as shown. Batch 2 adds authentication/authorization to all services; every protected call requires `Authorization: Bearer <access token>`.

**user-service (`/api/v1/users`)** — baseline; Batch 1 changes are in §0.
- `POST /register` — public; **baseline** accepted client `role`; Batch 1 ignores it and persists `ROLE_USER`.
- `POST /login` — public; returns access + refresh tokens.
- `POST /logout` — **baseline** `permitAll`; Batch 1 requires authentication and revokes access + refresh state.
- `POST /refresh-token` — public; **baseline** accepted any valid JWT; Batch 1 requires a refresh token and rotates it.
- `PUT /update`, `PUT /change-password`, `GET /me` — `ROLE_USER`; responses are DTOs in Batch 1.

**product-service (`/api/v1/products`)**
- `GET /{skuCode}`, `GET /?page=&size=&sort=` — public.
- `POST /`, `PUT /{skuCode}`, `DELETE /{skuCode}` — `ROLE_ADMIN` (Batch 2).

**inventory-service (`/api/v1/inventory`)**
- `GET /{skuCode}`, `GET /?page=&size=&sort=` — authenticated (Batch 2).
- `POST /`, `PUT /{skuCode}`, `DELETE /{skuCode}` — `ROLE_ADMIN` or `ROLE_INTERNAL` (Batch 2).

**cart-service (`/api/v1/cart`)**
- `GET /{userId}`, `POST /{userId}/add`, `DELETE /{userId}/remove/{productId}/quantity/{quantity}`, `DELETE /{userId}/clear`, `POST /checkout` — authenticated; `{userId}`/body `userId` must equal the token subject (Batch 2).

**order-service (`/api/v1/orders`)**
- `POST /` — authenticated; `userId` is derived from the token (Batch 2).
- `GET /{orderNumber}`, `PUT /{orderNumber}/cancel`, `GET /{orderNumber}/history` — owner only, or `ROLE_ADMIN`/`ROLE_INTERNAL` (Batch 2).
- `GET /user/{userId}?page=&size=&status=` — owner only, or `ROLE_ADMIN`/`ROLE_INTERNAL` (Batch 2).
- `PUT /{orderNumber}/status?status=` — `ROLE_ADMIN` or `ROLE_INTERNAL` (Batch 2).

### 2.4 Actual request flows (baseline; Batch 1/2 changes noted inline)

**Registration**
1. Client posts username/password/email/role.
2. user-service checks username existence (read-then-write race).
3. BCrypt-hashes the password, persists the entity, including the client role.
4. Sends a Kafka registration event (fire-and-forget).
5. Returns the JPA entity, including the password hash.

**Login**
1. `AuthenticationManager` verifies credentials against SQL Server.
2. Generates a 24-hour access JWT and 7-day refresh JWT (same key, no type/issuer/audience).
3. Stores the refresh token in Redis under `refresh_token:<username>`.
4. Sends a login event and returns both tokens.

**Cart add**
1. Client supplies `userId` in the path and `productId`, `quantity`, `price` in the body. **Batch 2:** the request must carry a valid access token and `userId` must equal the token subject.
2. cart-service upserts a `cart_items` row using the client price.
3. Controller publishes an add event (outside the service transaction).
4. Redis `cart` cache is updated/evicted by annotations.

**Checkout (as implemented)**
1. Client posts `userId` and payment method. **Batch 2:** the token subject must match `userId`.
2. cart-service loads the cart, sums the stored client prices, computes "purchasedProductIds".
3. Deletes all cart items.
4. Returns `paymentStatus = "SUCCESS"` (simulated).
5. Controller loads the now-empty cart and publishes an event with zero items/total.
6. No order is created, no stock is reserved, no payment is processed.

**Order create**
1. Client posts SKU/quantity/price items. **Batch 2:** the body no longer accepts `userId`; the controller passes the token subject.
2. order-service saves an order with a random UUID, status `CREATED`, the token subject, and the client items.
3. Sends `OrderEvent` after save.
4. No inventory/product/payment interaction.

### 2.5 Integration reality

| Expected integration | Status |
|---|---|
| Cart → Product (price lookup) | Not implemented |
| Cart → Inventory (reserve stock) | Not implemented |
| Cart → Order (create order) | Not implemented |
| Order → Inventory (reserve/release) | Not implemented |
| Order → Payment | Not implemented; cart fakes success |
| Order → Notification | Not implemented |
| Kafka consumers for saga steps | Not implemented |
| Kong → application JWT | Not implemented; incompatible credential models |
| Service discovery/config server | Not present |
| Distributed tracing | Not present |

---

## 3. Target architecture

The target keeps the five service boundaries (there is no evidence that splitting or merging them is necessary) and fixes the contracts between them.

```
                       ┌──────────────────────────────┐
   Clients ──────────► │ Edge: Kong / Ingress + WAF    │
                       │ TLS, rate limit, authn edge   │
                       └──────────────┬───────────────┘
                                      │ mTLS / private network
        ┌──────────────┬──────────────┼──────────────┬──────────────┐
        ▼              ▼              ▼              ▼              ▼
   user-service   catalog-service  inventory      cart          order
   (OIDC/JWT)     (product)        (reserve)      (pricing)     (saga/orchestration)
        │              │              │              │              │
        └──────────────┴──────────────┴──────────────┴──────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
              SQL Server        Redis (AUTH/TLS)   Kafka (SASL_SSL)
              per service       cache + sessions   outbox + DLQ
                    │
                    ▼
              OpenTelemetry → Prometheus / Grafana / Loki / alerts
```

### 3.1 Target principles

1. **Single source of truth per aggregate.** Product owns price; inventory owns availability; order owns the order state; cart owns intent. Copies are snapshots with provenance.
2. **Server-side authority.** Client-supplied price, user ID, and role are never trusted.
3. **Deny by default.** Every service is a resource server; only documented public reads.
4. **Transactional outbox.** Domain write and event record commit together; a relay publishes and retries with a DLQ.
5. **Idempotency everywhere.** Checkout/order creation and consumers are idempotent.
6. **Versioned schema.** Flyway migrations; `ddl-auto=validate`.
7. **Observability by default.** Correlation IDs, metrics, health, structured logs, traces.
8. **Reproducible environments.** Dockerfiles, Compose profiles, migrations, `.env.example`, CI.

### 3.2 Target checkout flow (synchronous + saga)

```
Client ──POST /checkout (Idempotency-Key)──► cart
  cart ──GET price/availability──► product + inventory
  cart ──create order (PENDING)──► order
  order ──reserve stock──► inventory (atomic, expiring reservation)
  order ──authorize payment──► payment provider (real integration)
  on success: order CONFIRMED, cart cleared, events published (outbox)
  on failure: release reservation, order CANCELLED, cart preserved, no charge
```

If a real payment provider is not in scope, the system must expose an explicit `PAYMENT_NOT_CONFIGURED`/`PENDING_PAYMENT` state and must not report success.

---

## 4. Key decisions and trade-offs

| Decision | Current | Target | Rationale |
|---|---|---|---|
| Auth token | HS256, shared secret, untyped | RS256/ES256 + JWKS, typed tokens | Multi-service verification without sharing a secret; revocation via short TTL + session state |
| Gateway auth | Kong JWT with separate credentials | Edge validates app issuer, or services validate and Kong handles edge concerns | Avoid two incompatible identity systems |
| Cart/order identity | Path/body `userId` | Principal-derived | Prevents IDOR |
| Pricing | Client-supplied | Product-service authority | Prevents fraud |
| Stock | CRUD only | Reservation + atomic decrement | Prevents oversell |
| Events | Fire-and-forget | Outbox + DLQ + idempotent consumers | Prevents loss/duplication |
| Schema | `ddl-auto=update` | Flyway + validate | Reproducibility and safety |
| Deployment | Infra-only Compose + IDE | Dockerfiles + Compose/Helm + CI | Reproducibility |
| Observability | Partial metrics/logs | Full metrics/logs/traces/alerts | Operability |

---

## 5. Non-goals (for now)

- Splitting or merging services without measured evidence.
- A service mesh, Kubernetes, or multi-region deployment before the P0/P1 batches are complete.
- Event sourcing or CQRS; a transactional outbox is sufficient for the current scale.
- Multi-tenancy, multi-currency, or complex tax handling until the single-currency flow is correct.
- Frontend, notification, or payment features without real integrations and operational ownership.

---

## 6. Repository map

```
nexgen/
├── docker-compose.yml            # infrastructure only; no app containers
├── prometheus/prometheus.yml     # scrapes order + cart only
├── loki/loki-config.yaml
├── promtail-config.yaml          # watches cart + Kong only
├── kong-automation/              # shell scripts; broken entrypoint
├── nexgen-server/
│   ├── user-service/user-service/
│   ├── inventory-service/inventory-service/
│   ├── product-service/product-service/
│   ├── cart-service/cart-service/
│   └── order-service/order-service/
├── docs/
│   ├── PRODUCTION_READINESS_AUDIT.md
│   ├── IMPLEMENTATION_ROADMAP.md
│   ├── ARCHITECTURE.md
│   ├── SECURITY.md
│   ├── OPERATIONS_RUNBOOK.md
│   └── DEPLOYMENT.md
└── README.md
```

Each service is a standalone Maven project with its own wrapper; there is no parent aggregator POM. A root aggregator is recommended in Batch 6 to simplify CI and dependency management.

---

## 7. Architecture risks

| Risk | Consequence | Mitigation |
|---|---|---|
| No clear pricing authority | Fraud, inconsistent totals | Product service as price authority; snapshot with provenance |
| No stock reservation | Oversell, angry customers | Atomic reservation with expiry and compensation |
| No idempotency | Duplicate orders/charges | Idempotency keys + unique constraints |
| Fire-and-forget events | Silent data loss | Transactional outbox + DLQ |
| Shared JWT secret | Hard rotation, broad blast radius | Asymmetric keys + JWKS |
| Direct service access | Gateway controls bypassed | Private networking, no published ports |
| `ddl-auto=update` | Schema drift, failed deploys | Flyway + validate |
| Partial observability | Slow incident response | Full scrape/log/trace coverage |

---

## 8. Related documents

- `docs/PRODUCTION_READINESS_AUDIT.md` — findings and evidence.
- `docs/IMPLEMENTATION_ROADMAP.md` — batch sequence and acceptance criteria.
- `docs/SECURITY.md` — threat model and security standards.
- `docs/OPERATIONS_RUNBOOK.md` — run and recovery procedures.
- `docs/DEPLOYMENT.md` — build, configure, and deploy.
