# NexGen E-Commerce Microservices Platform

NexGen is a learning-oriented e-commerce backend built with Spring Boot, SQL Server, Redis, Kafka, and Docker. It has six independently buildable services. The commercial core (checkout, stock reservation, order confirmation, payment authorization, notifications) is **event-driven over Kafka**; the API gateway is wired by Kong but has not been rehearsed end to end.

> **Project status (2026-10-29): NOT PRODUCTION READY.**
> The authoritative assessment is [`docs/PRODUCTION_READINESS_AUDIT.md`](docs/PRODUCTION_READINESS_AUDIT.md). It documents remaining gaps (deployment hardening, E2E/Kong/runtime verification, integration tests with live infra).
>
> Batches 1-8 implemented: user auth hardening (Batch 1), service authz across product/inventory/cart/order (Batch 2), checkout/order integrity with server-side pricing/stock, idempotency, reservations (Batch 3), Flyway migrations and constraints (Batch 4), cart outbox for durable events (Batch 5), Dockerfiles and `.env.example` (Batch 6), correlation IDs and actuator hardening (Batch 7), Testcontainers deps added (Batch 8). Live Docker/Testcontainers/Kong/E2E not verified. See [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md) before running or deploying anything.

## 📚 Documentation

- [`docs/PRODUCTION_READINESS_AUDIT.md`](docs/PRODUCTION_READINESS_AUDIT.md) — findings, evidence, severity, remediation plan.
- [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md) — batch sequence and acceptance criteria.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — as-is and target architecture.
- [`docs/SECURITY.md`](docs/SECURITY.md) — threat model and security standards.
- [`docs/OPERATIONS_RUNBOOK.md`](docs/OPERATIONS_RUNBOOK.md) — local run and failure procedures.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — configuration, build, and deployment blockers.

## 🔧 Technologies Used

- **Backend**: Spring Boot (Java 17+), Spring Security, Spring Data JPA, Spring Validation
- **Frontend**: ReactJS (Planned)
- **Database**: MS SQL Server (per service)
- **Cloud**: Microsoft Azure (Planned)
- **Cache**: Redis
- **Event Bus**: Apache Kafka (Confluent). The current Compose setup uses **PLAINTEXT**; SASL_SSL is not implemented.
- **API Gateway**: Kong Gateway, configured by the idempotent `kong-automation/setup-all.sh`. The scripts have been verified against a mock Admin API, not yet against a live Kong container.
- **DevOps**: Docker Compose for infrastructure only; there are no application Dockerfiles or CI yet.
- **Monitoring**: Spring Boot Actuator in four services; Prometheus/Grafana coverage is partial.
- **Observability**: File-based logging; Loki/Promtail cover only cart-service and Kong.
- **Resilience**: Spring Cloud Resilience4j in cart-service only; not a system-wide guarantee.
- **Documentation**: Swagger/OpenAPI (security schemes are currently configured only for user-service).
- **Orchestration Pattern**: Choreographed saga over Kafka (order → inventory → order → payment → order → cart/notification).

---

## 🧩 Microservices Overview

### 1. **User Service**
- **Authentication**: JWT access/refresh login, rotation and logout revocation (hardened in Batch 1)
- **Features**: Register, login, refresh, logout, update profile, change password, get current profile
- **Security**: Spring Security; access/refresh token types, issuer/audience checks, hashed refresh and blacklist state in Redis, server-assigned `ROLE_USER`
- **Events**: Typed JSON account events for registration, login, profile update and password change, written to a transactional outbox and published to `user-registration`, `user-login`, `user-profile-updated`, `user-password-changed`; consumed by notification-service
- **Caching**: User-profile caching was removed in Batch 1 to avoid stale/sensitive cache entries
- **Swagger**: Integrated; bearer scheme configured
- **Monitoring**: No Actuator dependency yet; Prometheus/Grafana are **not** wired for this service

### 2. **Inventory Service**
- **CRUD**: Create, update, delete, and get inventory items
- **Pagination**: Supported for listing inventory items
- **Events**: Owns stock. Consumes `order-created` to reserve, `order-confirmed` to deduct and `order-cancelled` to release, publishing `inventory-reserved` / `inventory-rejected` (the order service waits for those); also consumes `product-created` / `product-updated` / `product-deleted` and publishes `inventory-events` for the catalogue mirror
- **Caching**: Redis caching for get, update, and delete operations
- **Security**: Resource server (Batch 2). Reads require a valid access token; writes require `ROLE_ADMIN` or `ROLE_INTERNAL`
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Monitoring**: Actuator and Micrometer present; Prometheus does not scrape this service yet

### 3. **Product Service**
- **CRUD**: Create, update, delete, and get products
- **Pagination**: Supported for listing products
- **Events**: Publishes `product-created` / `product-updated` / `product-deleted` from the outbox; consumes `inventory-events` to keep its displayed quantity in step with the stock ledger
- **Caching**: Redis caching for get, update, and delete operations
- **Security**: Resource server (Batch 2). Product reads are public; writes require `ROLE_ADMIN`
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Monitoring**: Actuator and Micrometer present; Prometheus does not scrape this service yet

### 4. **Cart Service**
- **Features**: Add/remove items, update quantity, calculate total, simulated checkout
- **Integration**: Checkout asks the order service to create the order (the client needs an order number immediately) and keeps the cart until `order-confirmed` arrives, so a rejected reservation leaves the cart intact
- **Caching**: Redis caching for cart reads and writes
- **Security**: Resource server (Batch 2). All cart endpoints require a valid access token and only allow the authenticated subject to access its own cart
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Saga Participation**: Consumes `order-confirmed` (clear cart) and `product-deleted` (drop the product); publishes `cart-checkout` after an accepted checkout
- **Monitoring**: Actuator and Prometheus registry present; Prometheus scrapes this service

### 5. **Order Service**
- **Features**: Create, retrieve, cancel, and update orders; order status history
- **Events**: Publishes one `order-created` per order from the outbox, then `order-confirmed` / `order-cancelled` / `order-status-updated`; consumes `inventory-reserved` and `inventory-rejected` to drive the status transition
- **Payment**: `payment-service` consumes `order-confirmed` and publishes `payment-authorized` / `payment-failed`; the gateway is a `PaymentGateway` port whose default implementation is a deterministic **simulator** (no money, no credentials). Declines total amounts ending in `.99`. Swapping in a real PSP means one more implementation of that port.
- **Failure Handling**: `inventory-rejected` cancels the order with the rejection reason and publishes `order-cancelled`, which makes inventory release whatever it had reserved
- **Resilience**: Stock is no longer reserved over HTTP; the only synchronous call left is the product-price lookup when an order is created
- **Security**: Resource server (Batch 2). Creation derives `userId` from the access token; reads/cancel/history are owner-only unless the caller has `ROLE_ADMIN`/`ROLE_INTERNAL`; status updates require `ROLE_ADMIN`/`ROLE_INTERNAL`
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Monitoring**: Actuator and Prometheus registry present; Prometheus scrapes this service

### 6. **Notification Service**
- **Status**: Implemented as a pure Kafka consumer (no REST surface, no search or checkout logic)
- **Events consumed**: `cart-checkout`, `order-created`, `order-confirmed`, `order-cancelled`, `order-status-updated`, `user-registration`, `user-login`, `user-profile-updated`, `user-password-changed`; each notification is deduplicated by `eventId` and stored in `notifications`
- **Not included**: actual email/SMS delivery (no provider integration); the service records the message that would be sent

---

## ⚙️ Architecture

The Kafka paths below are implemented. Kong routing is declared by `kong-automation/setup-all.sh`, and cross-service runtime behaviour has not been verified because the infrastructure was not started.

```
               +--------------------------+
               |      Kong Gateway        |
               +------------+-------------+
                            |
        +-------------------+---------------------+
        |                   |                     |
+-------v-----+     +-------v-----+       +--------v-------+
|  User Svc   |     | Inventory   |       |  Product Svc   |
+-------------+     +-------------+       +----------------+
       |                   |                     |
       |                   |                     |
+------v-----+     +-------v-----+       +--------v-------+
|   Redis    |     |   Kafka     |<----->|  Order Svc     |
+------------+     +-------------+       +----------------+
                                ↑
                                |
                       +--------v--------+
                       | Notification Svc|
                       +-----------------+

Monitoring: Prometheus + Grafana
Logging: Centralized via structured logs
Database: MS SQL Server (Per service)
```

---

## 🔐 Security

* **All five services** validate the same HS256 access token issued by user-service (`iss`, `aud`, `exp`, `token_type=access`). Product, inventory, cart, and order use Spring Security resource-server configuration (Batch 2). Tokens are rejected if `token_type` is `refresh`, the issuer/audience is wrong, or the signature is invalid.
* **Role and ownership matrix:** product reads are public and writes require `ROLE_ADMIN`; inventory reads require authentication and writes require `ROLE_ADMIN` or `ROLE_INTERNAL`; cart access is self-only; order creation derives `userId` from the token, reads/cancel/history are owner-only unless privileged, and status updates require `ROLE_ADMIN` or `ROLE_INTERNAL`. Denials return JSON `401`/`403` rather than an HTML login page.
* **No admin provisioning exists yet.** Registration always assigns `ROLE_USER`, so admin/internal tokens must currently be minted out of band for testing. Do not add a public admin-registration path without a separate audited design.
* **Swagger/OpenAPI, `/actuator/health`, `/actuator/info`, and `/error` remain public** in the four resource-server services. Swagger must be disabled or protected before production.
* **Kafka is PLAINTEXT** and Redis has no authentication in the provided Compose setup; SQL Server falls back to `sa` if `DB_USERNAME` is omitted. These are infrastructure findings, not solved by Batch 2.
* **Kong is not part of the tested startup path.** `kong-automation/setup-all.sh` is idempotent and self-configuring (defaults for every port/URL, no `.env` required), but it has only been exercised against a mock Admin API; a live proxied request has not been verified.
* `JWT_SECRET` must be at least 32 bytes and identical across all services. It is validated at startup; do not ship the example value.
* CORS is an explicit allowlist per service via `CORS_ALLOWED_ORIGINS`; wildcard origins are not used.

**No production deployment should be made until the Critical findings in the audit are closed.**

## 📊 Monitoring & Observability

* Actuator is present in inventory, product, cart, and order; user-service currently lacks it.
* Prometheus scrapes cart and order only. Product and inventory are not scraped despite having Micrometer.
* Grafana has no provisioned dashboards in the repository.
* Loki/Promtail watch cart-service and Kong only. Logs are file-based with relative paths and no rotation policy.
* Custom metrics are partial and not documented as SLOs.

## 🛡 Resilience

* Resilience4j is used only in cart-service.
* Every service publishes domain events through a transactional outbox: the row commits with the state change, the publisher awaits the send result, retries every 2s and moves unrecoverable rows to FAILED after 10 attempts. Consumers retry via `DefaultErrorHandler` and dead-letter to `<topic>.DLT`.
* Synchronous calls remain only where the caller needs an immediate answer: cart→order (checkout command) and cart/order→product (price lookup). Stock reservation, order confirmation and cart clearing are asynchronous.
* No backpressure, rate limiting, or bulkhead configuration is validated in production conditions.

## 🔀 Event-Driven Saga Pattern (Choreography)

Checkout is a choreographed saga over Kafka:

* Order Service commits the order as `CREATED` and publishes `order-created` (items included).
* Inventory Service consumes it, reserves stock atomically and publishes `inventory-reserved`, or publishes `inventory-rejected` with the reason.
* Order Service consumes that answer: `inventory-reserved` → `CONFIRMED` + `order-confirmed`; `inventory-rejected` → `CANCELLED` + `order-cancelled`.
* Inventory consumes `order-confirmed` to deduct stock and `order-cancelled` to release it; cart consumes `order-confirmed` to clear itself; notification-service notifies the customer at every step.
* Compensation is the rejection path: a declined payment cancels the order (`payment-failed` → `order-cancelled`) and `order-cancelled` releases any partial reservation and refunds. `paymentStatus` moves `PENDING` → `AUTHORIZED` → (`REFUNDED`).

Every step is idempotent: events carry an `eventId` that consumers record in `processed_events`, reservations are keyed by order number, and status changes to an already-applied status are no-ops.


## 🚀 Getting Started

### Prerequisites
- Java 17+ (the repository has been built with Java 21 and Maven Wrapper 3.9.9)
- Docker & Docker Compose (optional but needed for local SQL Server/Redis/Kafka)
- SQL Server, Redis, and Kafka if not using Compose

### Current local setup

The repository does **not** contain a one-command working stack. The Compose file starts infrastructure containers only; there are no application Dockerfiles. Kong routing is wired by `kong-automation/setup-all.sh` but unrehearsed against a live gateway. Use [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) and [`docs/OPERATIONS_RUNBOOK.md`](docs/OPERATIONS_RUNBOOK.md) for the verified, step-by-step procedure.

Typical outline:

```bash
cp .env.example .env
# Edit .env: set strong database/Kong passwords and a random JWT_SECRET of at least 32 bytes.
docker compose up -d mssql redis zookeeper kafka
# Create the six per-service databases manually: userdb, inventorydb, productdb, cartdb, orderdb, notificationdb.
# Schemas are applied by Flyway migrations on startup (src/main/resources/db/migration).
```

Build and run one service at a time:

```bash
cd nexgen-server/user-service/user-service
./mvnw -DskipTests package
JWT_SECRET="<at-least-32-random-bytes>" java -jar target/user-service-1.0.0.jar
```

Every service must receive the same `JWT_SECRET`, plus `JWT_ISSUER=nexgen-user-service` and `JWT_AUDIENCE=nexgen-api`. Set `CORS_ALLOWED_ORIGINS` to a comma-separated browser origin allowlist. See [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) for the full variable list.

On Windows PowerShell, use `.\mvnw.cmd`. Each service's current test lifecycle is:

```powershell
cd nexgen-server/<service>/<service>
.\mvnw.cmd -B test
```

**Verified 2026-11-03:** 165 tests run, 160 passed, 5 skipped, 0 failures/errors across the six services. The five skips are the `*ApplicationTests` context tests, disabled with an explicit reason because they require SQL Server, Redis, and Kafka. `./mvnw -DskipTests package` succeeds for all six services. Runtime and cross-service behavior are **not** verified because the infrastructure was not started: no broker was contacted and no end-to-end checkout was executed.

---

## ✅ Implemented vs. not implemented

**Implemented**
- JWT access/refresh authentication on user-service (Batch 1), including rotation and revocation
- Resource-server JWT validation on product, inventory, cart, and order (Batch 2)
- Role-gated catalog/inventory writes and self-only cart access (Batch 2)
- Order ownership checks with token-derived `userId` (Batch 2)
- Server-side role assignment for registration; profile response DTOs
- Transactional outbox with a retrying, failure-aware publisher in every service, and idempotent consumers keyed on `eventId`
- Redis caching in product/cart/order; user-profile cache removed in Batch 1
- Swagger/OpenAPI in the five HTTP services (only user-service documents auth); notification-service has no REST surface by design
- CRUD APIs and pagination for products, inventory, and orders

**Not implemented / partial**
- Admin/internal account provisioning and audited admin flows
- A real payment provider (the simulator charges nothing; a live PSP also costs money)
- Database migrations or initialization scripts
- Dockerfiles, CI, or a working Kong setup
- An API gateway that has been rehearsed against a live Kong (the automation is fixed and idempotent, but not exercised end to end)
- Production observability (only partial Actuator/Prometheus coverage)
- Docker/Testcontainers integration and E2E tests (context tests are currently skipped)

---

## 🧪 Upcoming Work

The prioritized plan is in [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md). Highest-priority items:

- [x] Add authentication/authorization to all services and enforce resource ownership (Batch 2; isolated tests only)
- [ ] Replace client-trusted checkout with server-side price/stock validation
- [x] Make Kafka publishing reliable (outbox, awaited sends, retries, dead-letter) and consumers idempotent
- [ ] Add database migrations and remove exposed/default infrastructure credentials
- [ ] Add Dockerfiles, CI, and a tested Kong configuration
- [ ] Add Actuator, dashboards, and actionable alerts
- [ ] Add integration/E2E tests with Testcontainers
- [x] Implement the event-driven checkout saga, the payment consumer and the notification consumer

---

## 🧼 Engineering practices in the current code

- DTOs are used for user-service requests/responses; other services still expose entities in several endpoints.
- Exception handling exists, but not all services return consistent error contracts.
- Redis caching is used without a uniform TTL/invalidation strategy.
- Every event type has a producer and at least one consumer; events without a consumer were removed rather than left as noise.
- Logging is file-based; there is no correlation-ID propagation or centralized retention policy.
- The codebase is modular by service and the Kafka topology is implemented end to end in code; only automated cross-service tests are missing.
- **Not yet achieved:** verified end-to-end security (the gateway and runtime paths are not tested), system-wide resilience, or production deployment.

---

## 📂 Project Structure

```
nexgen/
├── nexgen-server/
│   ├── user-service/user-service/
│   ├── inventory-service/inventory-service/
│   ├── product-service/product-service/
│   ├── cart-service/cart-service/
│   ├── notification-service/notification-service/
│   └── order-service/order-service/
├── kong-automation/          # idempotent gateway setup scripts
├── prometheus/               # Prometheus config
├── loki/                     # Loki config
├── promtail-config.yaml
├── logs/                     # local log output (ignored by git)
├── docs/                     # audit, architecture, security, runbook, deployment
├── docker-compose.yml        # infrastructure only
└── .env.example              # configuration template (no real secrets)
```

There is no application `api-gateway/` source directory; the gateway is Kong, declared by the scripts in `kong-automation/`.

---

## 👨‍💻 Author

**Vikas Dangi**  
Java Developer | Spring Boot | React | Microservices | AWS

📧 Connect with me on [LinkedIn](https://www.linkedin.com/in/vikasdangi/)
---
