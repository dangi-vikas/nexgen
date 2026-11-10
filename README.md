# NexGen E-Commerce Microservices Platform

NexGen is a learning-oriented e-commerce backend built with Spring Boot, SQL Server, Redis, Kafka, Kong and Docker. It has **seven** independently buildable services. The commercial core (checkout, stock reservation, order confirmation, payment authorisation, notification) is **event-driven over Kafka**.

> **Project status (2026-11-06): verified end to end, still not production ready.**
> The whole checkout saga, the gateway and the notification pipeline were exercised against live Docker infrastructure: `scripts/e2e-smoke.sh` passes **35/35 checks**. Production gaps that remain are listed under [Known limitations](#-known-limitations).
>
> The authoritative readiness assessment is [`docs/PRODUCTION_READINESS_AUDIT.md`](docs/PRODUCTION_READINESS_AUDIT.md).

## 📚 Documentation

- **Per-service documentation**: [`docs/services/`](docs/services/) — endpoints, topics, configuration and implementation notes for each service.
  [`user`](docs/services/user-service.md) ·
  [`product`](docs/services/product-service.md) ·
  [`inventory`](docs/services/inventory-service.md) ·
  [`cart`](docs/services/cart-service.md) ·
  [`order`](docs/services/order-service.md) ·
  [`payment`](docs/services/payment-service.md) ·
  [`notification`](docs/services/notification-service.md)
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — as-is and target architecture.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — configuration, build and deployment notes.
- [`docs/OPERATIONS_RUNBOOK.md`](docs/OPERATIONS_RUNBOOK.md) — local run and failure procedures.
- [`docs/PRODUCTION_READINESS_AUDIT.md`](docs/PRODUCTION_READINESS_AUDIT.md) — findings and remediation plan.
- [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md) — batch sequence and acceptance criteria.
- [`docs/SECURITY.md`](docs/SECURITY.md) — threat model and security standards.
- [`nexgen-ui/README.md`](nexgen-ui/README.md) — NexGen storefront: setup, environment, routes and data adapter.

## 🔧 Technologies Used

- **Backend**: Spring Boot 3.4 (Java 17+), Spring Security, Spring Data JPA, Spring Validation, Spring Kafka
- **Databases**: SQL Server, one database per service, schema owned by Flyway migrations
- **Cache**: Redis (cart reads, plus caching in product/inventory/order)
- **Event bus**: Apache Kafka (Confluent 7.5, ZooKeeper mode, PLAINTEXT)
- **API gateway**: Kong 3.5, **Postgres-backed** (declarative DB-less mode is deliberately not used), configured idempotently by `kong-automation/setup-all.sh`
- **Email**: Mailpit (SMTP `1025`, web UI `8025`) — no external provider, no credentials
- **Payments**: in-process simulator behind a `PaymentGateway` port
- **Observability**: Actuator and Micrometer in all seven services, Prometheus scraping every one of them, and Grafana dashboards plus Loki log shipping provisioned from the repository
- **Resilience**: Resilience4j in cart-service; every service uses a transactional outbox with an at-least-once publisher
- **DevOps**: Docker Compose for infrastructure, a multi-stage Dockerfile per service, and GitHub Actions CI that tests every module and runs the E2E suite

## 🧩 Services at a glance

| Service | Port | Responsibility | Docs |
| --- | --- | --- | --- |
| **user-service** | 8081 | Registration, JWT access/refresh login, rotation and revocation, profile | [details](docs/services/user-service.md) |
| **inventory-service** | 8082 | Stock ledger and reservations | [details](docs/services/inventory-service.md) |
| **product-service** | 8083 | Catalogue and server-side prices | [details](docs/services/product-service.md) |
| **order-service** | 8084 | Orders, status history, saga driver | [details](docs/services/order-service.md) |
| **payment-service** | 8085 | Charges and refunds (Kafka only) | [details](docs/services/payment-service.md) |
| **cart-service** | 8086 | Cart CRUD, totals, checkout command | [details](docs/services/cart-service.md) |
| **notification-service** | 8087 | Customer messages and e-mail delivery (Kafka only) | [details](docs/services/notification-service.md) |

Kong listens on **8000** (proxy) and **8001** (admin). Infrastructure: SQL Server `1433`, Redis `6379`, Kafka `9092`, ZooKeeper `2181`, Mailpit `1025`/`8025`, Kong's Postgres on its own container.

Two cross-cutting behaviours are easy to miss and are documented per service:

- **Payment is simulated.** `payment-service` owns a `PaymentGateway` port; the default implementation charges nothing and **declines any total whose cents are `.99`** — the deterministic trigger for the compensation path in the E2E suite.
- **Notifications are delivered, not just recorded.** `notifications` is a delivery outbox (`PENDING` → `SENT` / `FAILED`) with addresses resolved from `user_contacts`, sent through Mailpit locally.

## 🔀 Event-Driven Saga Pattern (Choreography)

1. `POST /api/v1/orders` (or cart checkout) prices the items server-side and commits the order as `CREATED`, publishing `order-created` from the transactional outbox.
2. Inventory reserves atomically and answers `inventory-reserved` or `inventory-rejected`.
3. Order service moves `CREATED → CONFIRMED` and publishes `order-confirmed`, or `CREATED → CANCELLED` with `order-cancelled`.
4. `order-confirmed` fans out: inventory deducts the reservation, payment charges the order total, cart-service clears the buyer's cart, notification-service e-mails the customer.
5. Payment answers `payment-authorized` (order becomes `PAID`) or `payment-failed` (order is `CANCELLED`, which makes inventory release the reservation and payment record the failure).
6. Cancelling a paid order publishes `order-cancelled` → `payment-refunded`.

Consumers are idempotent: every event carries an `eventId` recorded in `processed_events`, reservations are keyed by order number, and re-applying a status is a no-op. Order creation is also idempotent per `idempotencyKey`.

## ⚙️ Architecture

```
                        +--------------------------+
        browser / curl  |      Kong Gateway :8000   |   admin API :8001 (Postgres-backed)
                        +------------+-------------+
                                     |
     +---------------+---------+-----+-----+---------+---------------+
     |               |               |               |               |
+----v----+   +------v-----+   +-----v-----+   +-----v-----+   +-----v------+
|  User   |   |  Product   |   | Inventory |   |   Order   |   |    Cart    |
| :8081   |   |  :8083     |   |  :8082    |   |  :8084    |   |   :8086    |
+----+----+   +------+-----+   +-----+-----+   +-----+-----+   +-----+------+
     |               |               |               |               |
     +---------------+-------+-------+---------------+---------------+
                             |
                  +----------v-----------+
                  |   Kafka (7 topics +  |      +------------------+
                  |   consumer groups)   |<---->| payment-service  | :8085
                  +----------+-----------+      +------------------+
                             |
                  +----------v-----------+
                  | notification-service | :8087  --> Mailpit :1025 / UI :8025
                  +----------------------+
```

Every service has its own SQL Server database (`userdb`, `productdb`, `inventorydb`, `cartdb`, `orderdb`, `paymentdb`, `notificationdb`).

## 📊 Monitoring & Observability

- Actuator is present in all seven services; `/actuator/health` is what the end-to-end readiness check polls, and `liveness`/`readiness` groups are enabled.
- Prometheus scrapes **all seven services** plus itself (`prometheus/prometheus.yml`). Every service exposes Micrometer metrics at `/actuator/prometheus`, and that path is explicitly permitted in each `SecurityConfig` — without it the scrape gets a 401 and the target is reported down, while `/actuator/metrics` stays behind authentication.
- Grafana is provisioned from the repository (`monitoring/grafana`): Prometheus and Loki datasources plus the **NexGen — Services & Checkout Saga** dashboard (services up, request rate, p95 latency, 5xx rate, JVM heap, order-service logs). Nothing is clicked in by hand.
- Loki/Promtail tail `logs/<service>/application.log` for all seven services and Kong; Loki keeps 7 days and enforces it through the compactor, and Promtail's position file lives in the mounted logs directory so a restart does not replay the whole history.
- Logs are structured JSON carrying a `correlationId`, and the gateway echoes it as `X-Correlation-Id`, so one checkout can be followed across services by that id.
- Custom metrics are partial and are not documented as SLOs.

## 🛡 Resilience

- Resilience4j (circuit breaker, rate limiter, timers) is configured in **cart-service only**; it is not a system-wide guarantee.
- Every service publishes domain events through a transactional outbox: the row commits with the state change, the publisher awaits the send result, retries every 2s and moves unrecoverable rows to `FAILED` after 10 attempts. Consumers retry via `DefaultErrorHandler` and dead-letter to `<topic>.DLT`.
- Synchronous calls remain only where the caller needs an immediate answer: cart→order (checkout command) and order→product (price lookup). Stock reservation, order confirmation, payment and cart clearing are asynchronous.
- Kafka runs as a single broker locally, with the offsets and transaction-state replication factors pinned to `1`; without that a consumer group cannot form at all.
- Cache invalidation is part of the resilience story, not an afterthought: the cart's Kafka reaction evicts the Redis entry as well as deleting rows, so a cleared cart cannot be served stale from cache.
- Payment failures are recoverable by design: the simulator declines deterministically, order-service cancels, inventory releases, and payment records the failure.
- No backpressure, bulkhead or load-shedding configuration has been validated under production load.

## ✅ Verification status (2026-11-06)

`./scripts/e2e-smoke.sh` runs the full stack for real and exits non-zero on failure. Last run: **35 passes, 0 failures**.

| Section | What is proved |
| --- | --- |
| 0. Readiness | all seven `/actuator/health` endpoints answer `200` |
| 1. Identity | registration, login token, bootstrap admin sign-in, promotion through the API, and both escalation attempts refused |
| 2. Catalogue/stock | admin creates a product and stocks it through the services |
| 3. Authorisation | anonymous write `401`, `ROLE_USER` write `403`, foreign cart `401` |
| 4. Cart | add through Redis-backed cart, quantity read back |
| 5. Saga | order created, idempotent replay returns the same order, order reaches `PAID`, stock 10 → 8, cart cleared by `order-confirmed` |
| 6. Notification | Mailpit receives an e-mail for the buyer |
| 7. Gateway | product read through Kong, authorized order read through Kong, anonymous order read `401`, `X-Correlation-Id` echoed, CORS allows the configured origin and never reflects another |
| 8. Compensation | a `.99` total is declined, order becomes `CANCELLED`, reservation released (5 units back) |

The suite needs `bash`, `curl`, `node` (for JSON without `jq`) and the running services plus infrastructure. Admin provisioning goes through the API using `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD`, so it no longer touches SQL Server directly.

## 🚀 Getting Started

### Prerequisites

- Java 17+ (built and verified with Java 21, Maven Wrapper 3.9.9)
- Docker Desktop
- Bash; on Windows use **Git Bash** (the scripts are POSIX)

### 1. Configure

```bash
cp .env.example .env
# set strong SQL/Kong passwords and a random JWT_SECRET of at least 32 bytes
```

Key variables: `JWT_SECRET`, `JWT_ISSUER=nexgen-user-service`, `JWT_AUDIENCE=nexgen-api`, `KONG_PG_PASSWORD`, `SMTP_HOST=localhost`, `SMTP_PORT=1025`, `NOTIFICATIONS_FROM`, plus the `*_PORT` values. `.env` is gitignored.

### 2. Start infrastructure

```bash
docker compose up -d mssql redis mailpit zookeeper kafka kong-db
# Kong runs its own migrations once, then start the gateway
docker compose up -d kong
```

Kafka is a **single broker**, so `docker-compose.yml` sets `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`. Without it `__consumer_offsets` demands three replicas, no consumer group can form, and events are published but never consumed.

### 3. Create the seven databases

```bash
for db in userdb productdb inventorydb cartdb orderdb paymentdb notificationdb; do
  docker exec mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$DB_PASSWORD" -C \
    -Q "IF DB_ID('$db') IS NULL CREATE DATABASE $db"
done
```

Schemas are applied by Flyway on startup (`src/main/resources/db/migration` in every service).

### 4. Build and run

Build every module first, then start each service **from its own module directory** — `logback-spring.xml` writes to `../../../logs/<service>/`:

```bash
cd nexgen-server/<service>/<service>
./mvnw -B -DskipTests package
java -jar target/<artifact>.jar     # user-service and product-service are 1.0.0.jar, the other five *-0.0.1-SNAPSHOT.jar
```

Each service reads `DB_NAME` for its own database. See the per-service pages under [`docs/services/`](docs/services/) and [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) for the full variable list.

### 5. Or run the services as containers

Each service has its own multi-stage Dockerfile; the build context is the module directory.

```bash
docker build -t nexgen/user-service nexgen-server/user-service/user-service
# … same pattern for product, inventory, cart, order, payment and notification

docker run --rm -p 8081:8081 \
  -e DB_HOST=host.docker.internal -e DB_NAME=userdb \
  -e JWT_SECRET="$JWT_SECRET" -e KAFKA_HOST=host.docker.internal \
  -e REDIS_HOST=host.docker.internal -e BOOTSTRAP_ADMIN_USERNAME=admin \
  -e BOOTSTRAP_ADMIN_PASSWORD="$BOOTSTRAP_ADMIN_PASSWORD" \
  nexgen/user-service
```

The runtime image is a JRE with a non-root user; nothing is compiled at runtime. Point `DB_HOST`, `KAFKA_HOST` and `REDIS_HOST` at `host.docker.internal` (Docker Desktop) or at your infrastructure's hostname.

### 6. Configure the gateway

```bash
./kong-automation/setup-all.sh
```

The script is idempotent: services and routes are `PUT` by name, plugins are looked up and `PATCH`ed or created **in the right scope** (a service plugin must be created at `/services/<name>/plugins`, never globally). Re-running it changes nothing. Result: 5 services, 5 routes (`/api/v1/users|products|cart|orders|inventory`) and 12 plugins (2 global + 2 per service).

Kong forwards to the host through `extra_hosts: host.docker.internal:host-gateway`, so services must be running on the host.

### 7. Run the end-to-end suite

```bash
./scripts/e2e-smoke.sh
```

### Unit tests

```bash
cd nexgen-server/<service>/<service> && ./mvnw -B test
```

Per-service counts at the last full run: user 54, order 33, inventory 26, product 26, cart 23, payment 5, notification 3.

## ⚠️ Known limitations

- **Payments are simulated.** No PSP, no real settlement; the `.99` decline rule is the only failure mode.
- **Kafka is PLAINTEXT, Redis has no auth, SQL Server uses `sa`** in the local Compose setup. Acceptable locally, never in production.
- `JWT_SECRET` must be at least 32 bytes and identical across services; the example value must never ship.
- Monitoring raises no alerts: Prometheus keeps its 15-day default and Loki 7 days, but there are no alert rules or notification channels, so nobody is paged when the stack misbehaves.
- Swagger is public in the HTTP services and documents auth only for user-service.

**No production deployment should be attempted while these hold.**

## ✅ Implemented vs. not implemented

**Implemented**

- JWT access/refresh authentication on user-service, including rotation and revocation
- Resource-server JWT validation on product, inventory, cart and order; role-gated writes and self-only cart access
- Order ownership checks with token-derived `userId`; server-side role assignment on registration
- Admin provisioning: a one-shot bootstrap administrator plus an admin-only role change endpoint, with the caller's own role refused and refresh tokens revoked on every change
- Transactional outbox with a retrying, failure-aware publisher in every service, and idempotent consumers keyed on `eventId`
- Event-driven checkout saga including stock reservation, payment authorisation, cart clearing and customer notification
- Flyway migrations in all seven services, with constraints that match the domain
- Redis caching in cart, product, inventory and order
- Mailpit-backed e-mail delivery outbox in notification-service
- Swagger/OpenAPI in the five HTTP services (auth documented only for user-service)
- Actuator health/readiness in every service, used by the E2E readiness check
- An end-to-end verification script covering 35 checks, including the decline/compensation path
- A multi-stage Dockerfile per service (non-root runtime) and a CI pipeline that builds and tests each module, then runs the full E2E suite

**Not implemented / partial**

- A dedicated audit trail for administrative actions (they are logged, but not stored in their own table)
- A real payment provider (the simulator charges nothing; a live PSP also costs money)
- Automated infrastructure provisioning (infrastructure is one Compose file; images are built per service)
- Alerting and retention: metrics and logs are collected and dashboarded, but nothing pages and nothing expires
- Testcontainers integration tests in CI (the E2E run is manual)
- Load, soak and failure-injection testing

## 🧪 Upcoming Work

The prioritized plan is in [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md). Highest-priority items:

- [x] Add authentication/authorization to all services and enforce resource ownership
- [x] Make Kafka publishing reliable (outbox, awaited sends, retries, dead-letter) and consumers idempotent
- [x] Implement the event-driven checkout saga, the payment consumer and the notification consumer
- [x] Add Flyway migrations and align constraints with the domain
- [x] Verify the Kong configuration and the full checkout flow end to end
- [x] Add Dockerfiles and a CI pipeline that builds, tests and runs the E2E suite
- [x] Add admin provisioning: bootstrap administrator and admin-only, logged role changes
- [x] Add provisioned Grafana dashboards for the services and the checkout saga
- [ ] Add alert rules and notification routing
- [ ] Move the E2E suite onto Testcontainers so it runs anywhere

## 🧼 Engineering practices in the current code

- DTOs are used for user-service requests/responses; several other services still expose entities from some endpoints.
- Exception handling exists, but not every service returns the same error contract; `GlobalExceptionHandler` maps validation, not-found and data-access failures to distinct statuses.
- Redis caching is used without a uniform TTL/invalidation strategy — the cart learned this the hard way, so cache eviction is now part of the reaction path, not an afterthought.
- Every event type has a producer and at least one consumer; events without a consumer were removed rather than left as noise.
- Logging is structured JSON with a correlation id propagated through HTTP and Kafka (`X-Correlation-Id`), but retention is still file-based.
- Status transitions are guarded in code *and* in the database; the two are kept in step by migrations rather than trusting one of them.
- Outbox rows use the event id as their primary key, so the id the producer writes and the id consumers deduplicate on are the same value.
- The codebase is modular by service and the Kafka topology is implemented end to end; the remaining gap is automated cross-service testing outside a developer machine.

## 📂 Project Structure

```
nexgen/
├── nexgen-server/
│   ├── user-service/user-service/
│   ├── product-service/product-service/
│   ├── inventory-service/inventory-service/
│   ├── cart-service/cart-service/
│   ├── order-service/order-service/
│   ├── payment-service/payment-service/
│   └── notification-service/notification-service/   # each module has a Dockerfile
├── kong-automation/          # idempotent gateway setup
├── scripts/e2e-smoke.sh      # end-to-end verification (35 checks)
├── scripts/ci-e2e.sh         # infrastructure + services + gateway + E2E (used by CI)
├── .github/workflows/ci.yml  # build/test matrix, then the end-to-end job
├── prometheus/  loki/  promtail-config.yaml
├── logs/                     # local log output (gitignored)
├── docs/                     # architecture, security, runbook, deployment, audit
│   └── services/             # one page per service
├── docker-compose.yml        # infrastructure only
└── .env.example              # configuration template (no real secrets)
```

There is no `api-gateway/` module: the gateway is Kong, declared by `kong-automation/`.

## 👨‍💻 Author

**Vikas Dangi**
Java Developer | Spring Boot | React | Microservices | AWS

📧 Connect with me on [LinkedIn](https://www.linkedin.com/in/vikasdangi/)
