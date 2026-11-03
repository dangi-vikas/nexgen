# NexGen E-Commerce Microservices Platform

NexGen is a learning-oriented e-commerce backend built with Spring Boot, SQL Server, Redis, Kafka, and Docker. It has five independently buildable services. The target design is event-driven and gateway-fronted, but several integrations described below are **not implemented yet**.

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
- **API Gateway**: Kong Gateway (automation scripts are currently broken; see audit C-08)
- **DevOps**: Docker Compose for infrastructure only; there are no application Dockerfiles or CI yet.
- **Monitoring**: Spring Boot Actuator in four services; Prometheus/Grafana coverage is partial.
- **Observability**: File-based logging; Loki/Promtail cover only cart-service and Kong.
- **Resilience**: Spring Cloud Resilience4j in cart-service only; not a system-wide guarantee.
- **Documentation**: Swagger/OpenAPI (security schemes are currently configured only for user-service).
- **Orchestration Pattern**: Saga is **not implemented**; see audit findings C-06, C-07, and H-05.

---

## 🧩 Microservices Overview

### 1. **User Service**
- **Authentication**: JWT access/refresh login, rotation and logout revocation (hardened in Batch 1)
- **Features**: Register, login, refresh, logout, update profile, change password, get current profile
- **Security**: Spring Security; access/refresh token types, issuer/audience checks, hashed refresh and blacklist state in Redis, server-assigned `ROLE_USER`
- **Events**: Fire-and-forget Kafka messages for registration, login, profile update, and password change
- **Caching**: User-profile caching was removed in Batch 1 to avoid stale/sensitive cache entries
- **Swagger**: Integrated; bearer scheme configured
- **Monitoring**: No Actuator dependency yet; Prometheus/Grafana are **not** wired for this service

### 2. **Inventory Service**
- **CRUD**: Create, update, delete, and get inventory items
- **Pagination**: Supported for listing inventory items
- **Events**: Kafka messages for create/update/delete and stock updates
- **Caching**: Redis caching for get, update, and delete operations
- **Security**: Resource server (Batch 2). Reads require a valid access token; writes require `ROLE_ADMIN` or `ROLE_INTERNAL`
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Monitoring**: Actuator and Micrometer present; Prometheus does not scrape this service yet

### 3. **Product Service**
- **CRUD**: Create, update, delete, and get products
- **Pagination**: Supported for listing products
- **Events**: Kafka messages for create/update/delete
- **Caching**: Redis caching for get, update, and delete operations
- **Security**: Resource server (Batch 2). Product reads are public; writes require `ROLE_ADMIN`
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Monitoring**: Actuator and Micrometer present; Prometheus does not scrape this service yet

### 4. **Cart Service**
- **Features**: Add/remove items, update quantity, calculate total, simulated checkout
- **Integration**: Product/Inventory/Order integration is **not implemented**; checkout still trusts client-supplied prices and returns simulated `SUCCESS` (audit C-06)
- **Caching**: Redis caching for cart reads and writes
- **Security**: Resource server (Batch 2). All cart endpoints require a valid access token and only allow the authenticated subject to access its own cart
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Saga Coordinator**: **Not implemented**
- **Monitoring**: Actuator and Prometheus registry present; Prometheus scrapes this service

### 5. **Order Service**
- **Features**: Create, retrieve, cancel, and update orders; order status history
- **Events**: Order events are published, but inventory/payment/notification consumers do not exist
- **Payment**: **Not implemented**; there is no payment provider integration
- **Failure Handling**: Compensating transactions are **not implemented**
- **Resilience**: No service-to-service calls; retry/circuit breaker are **not wired**
- **Security**: Resource server (Batch 2). Creation derives `userId` from the access token; reads/cancel/history are owner-only unless the caller has `ROLE_ADMIN`/`ROLE_INTERNAL`; status updates require `ROLE_ADMIN`/`ROLE_INTERNAL`
- **Swagger**: Integrated; bearer scheme is **not yet documented**
- **Monitoring**: Actuator and Prometheus registry present; Prometheus scrapes this service

### 6. **Notification Service** *(Planned)*
- **Status**: Not implemented; no source directory or deployed consumer exists
- **Planned features**: Email/SMS/in-app notifications, Kafka subscriptions, retry
- **Swagger/Monitoring**: Not present

---

## ⚙️ Architecture

The diagram below is the **target** design. Kong, Notification, and the synchronous integration paths are not working end-to-end in the current code (see audit C-08 and C-06).

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
* **Kong is not integrated.** The setup scripts reference missing files and are not part of the tested startup path.
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
* The Kafka retry queue is not a reliable outbox; entries can be lost and there is no dead-letter topic.
* There are no service-to-service HTTP calls, so timeout/circuit-breaker claims for downstream dependencies are not applicable yet.
* No backpressure, rate limiting, or bulkhead configuration is validated in production conditions.

## 🔀 Event-Driven Saga Pattern (Choreography)

The Saga is **not implemented**. There is no payment service, no inventory reservation/compensation consumer, and no notification consumer. Order events are published fire-and-forget. The target flow is:

* Order Service publishes an order event.
* Inventory Service would reserve stock and publish a result.
* A payment component would authorize payment.
* Notification Service would send confirmation.
* Compensation would run on failure.

Do not rely on this flow until the audit's Critical/High eventing findings are closed.


## 🚀 Getting Started

### Prerequisites
- Java 17+ (the repository has been built with Java 21 and Maven Wrapper 3.9.9)
- Docker & Docker Compose (optional but needed for local SQL Server/Redis/Kafka)
- SQL Server, Redis, and Kafka if not using Compose

### Current local setup

The repository does **not** contain a one-command working stack. The Compose file starts infrastructure containers only; there are no application Dockerfiles. Kong automation is currently broken. Use [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) and [`docs/OPERATIONS_RUNBOOK.md`](docs/OPERATIONS_RUNBOOK.md) for the verified, step-by-step procedure.

Typical outline:

```bash
cp .env.example .env
# Edit .env: set strong database/Kong passwords and a random JWT_SECRET of at least 32 bytes.
docker compose up -d mssql redis zookeeper kafka
# Create the five per-service databases manually. No SQL init/migration scripts exist.
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

**Verified 2026-10-26:** 161 tests run, 156 passed, 5 skipped, 0 failures/errors across the five services. The five skips are the `*ApplicationTests` context tests, disabled with an explicit reason because they require SQL Server, Redis, and Kafka; they will be replaced by Docker/Testcontainers integration tests in a later batch. `.\mvnw.cmd -B -DskipTests package` succeeds for all five services. Runtime and cross-service behavior are **not** verified because the infrastructure was not started.

---

## ✅ Implemented vs. not implemented

**Implemented**
- JWT access/refresh authentication on user-service (Batch 1), including rotation and revocation
- Resource-server JWT validation on product, inventory, cart, and order (Batch 2)
- Role-gated catalog/inventory writes and self-only cart access (Batch 2)
- Order ownership checks with token-derived `userId` (Batch 2)
- Server-side role assignment for registration; profile response DTOs
- Kafka event publishing from services (fire-and-forget)
- Redis caching in product/cart/order; user-profile cache removed in Batch 1
- Swagger/OpenAPI in the five services (only user-service documents auth)
- CRUD APIs and pagination for products, inventory, and orders

**Not implemented / partial**
- Admin/internal account provisioning and audited admin flows
- Server-side pricing, stock reservation, and real payment processing during checkout
- Order idempotency and reliable event delivery/outbox
- Database migrations or initialization scripts
- Dockerfiles, CI, or a working Kong setup
- Notification service, saga orchestration, and compensation
- Production observability (only partial Actuator/Prometheus coverage)
- Docker/Testcontainers integration and E2E tests (context tests are currently skipped)

---

## 🧪 Upcoming Work

The prioritized plan is in [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md). Highest-priority items:

- [x] Add authentication/authorization to all services and enforce resource ownership (Batch 2; isolated tests only)
- [ ] Replace client-trusted checkout with server-side price/stock validation
- [ ] Make order creation idempotent and make Kafka publishing reliable
- [ ] Add database migrations and remove exposed/default infrastructure credentials
- [ ] Add Dockerfiles, CI, and a tested Kong configuration
- [ ] Add Actuator, dashboards, and actionable alerts
- [ ] Add integration/E2E tests with Testcontainers
- [ ] Implement notification/payment/saga components only after critical fixes

---

## 🧼 Engineering practices in the current code

- DTOs are used for user-service requests/responses; other services still expose entities in several endpoints.
- Exception handling exists, but not all services return consistent error contracts.
- Redis caching is used without a uniform TTL/invalidation strategy.
- Kafka publishing is decoupled but not transactional or retried safely.
- Logging is file-based; there is no correlation-ID propagation or centralized retention policy.
- The codebase is modular by service, but several documented integration claims are not implemented.
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
│   └── order-service/order-service/
├── kong-automation/          # currently non-functional setup scripts
├── prometheus/               # Prometheus config
├── loki/                     # Loki config
├── promtail-config.yaml
├── logs/                     # local log output (ignored by git)
├── docs/                     # audit, architecture, security, runbook, deployment
├── docker-compose.yml        # infrastructure only
└── .env.example              # configuration template (no real secrets)
```

There is no `notification-service/` or application `api-gateway/` source directory yet.

---

## 👨‍💻 Author

**Vikas Dangi**  
Java Developer | Spring Boot | React | Microservices | Azure

📧 Connect with me on [LinkedIn](https://www.linkedin.com/in/vikasdangi/)
---
