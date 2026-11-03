# NexGen Production Readiness Audit

**Audit date:** 2026-10-24  
**Baseline commit:** `bf41b37e579cc7bd34f3828bc7b707f0e539f80a` (`master`, last commit 2025-05-26)  
**Repository:** `https://github.com/dangi-vikas/nexgen`  
**Auditor:** automated source and build inspection  
**Status:** Phase 1 complete, plus uncommitted Batch 1 (user-service) and Batch 2 (service authentication/authorization) working-tree changes. **Not production ready.** No production deployment should be attempted until the Critical findings below are closed.

> Everything in this document was confirmed by reading the tracked source, running the Maven builds, and running the tests that do not require external infrastructure. Runtime exploitation against a live stack was **not** performed: Docker was not started in this environment and the services require SQL Server, Redis, and Kafka to start. Items that could not be verified are explicitly marked **Unverified**.

---

## 1. Scope and method

### 1.1 What was reviewed

- All 128 Java files (~4,450 lines) across the 5 Spring Boot services.
- All 5 `pom.xml` files and Maven wrapper configuration.
- All `application.yml` / `application.yaml` files.
- All controllers, services, repositories, entities, DTOs, security/JWT code, Kafka producers/consumers, exception handlers, cache configs, metrics, and tests.
- `docker-compose.yml`, `prometheus/prometheus.yml`, `loki/loki-config.yaml`, `promtail-config.yaml`.
- `kong-automation/**` shell scripts.
- `README.md`, `.gitignore`, and the tracked file tree.

### 1.2 What was executed

| Check | Command | Result |
|---|---|---|
| Toolchain | `java -version`, `.\mvnw.cmd -version` | Java 21, Maven Wrapper 3.9.9 |
| Compile/package, all 5 services | `.\mvnw.cmd -B -q -DskipTests package` | Exit 0 for all 5 |
| User unit tests | `-Dtest=UserServiceTest test` | 7/7 pass |
| Product unit/slice tests | `-Dtest=ProductServiceTest,KafkaProducerServiceTest,ProductControllerTest test` | 18 run, 16 pass, 2 errors (`ProductMetrics` null) |
| Inventory unit/slice tests | `-Dtest=InventoryServiceImplTest,KafkaInventoryProducerTest,InventoryControllerTest test` | 16/16 pass |
| Cart unit tests | `-Dtest=CartServiceImplTest test` | 9 run, 4 failures, 1 error |
| Order unit tests | `-Dtest=OrderServiceImplTest test` | 11 run, 1 failure, 2 errors (`MeterRegistry` null) |
| Full test suites | `.\mvnw.cmd -B "-Dmaven.test.failure.ignore=true" test` | Did not complete within 15 minutes per service; `@SpringBootTest` waits on absent SQL Server/Redis/Kafka |
| Docker / integration / E2E | `docker version` | Docker CLI unavailable — **Unverified** |
| Live Kong / TLS / SASL_SSL | requires running stack | **Unverified** |

### 1.3 Interpretation of the baseline

- Compilation success only proves the code builds. It does **not** prove the services start, because startup requires external infrastructure and configuration that is not supplied by the repository.
- The isolated test results are the only reliable automated signal. Several failures are caused by tests that no longer match the implementation, not by production-code defects; both categories are reported.
- The full `@SpringBootTest` suites are part of the required verification gate and currently unusable without infrastructure. This is itself a release blocker.

---

## 2. Executive summary

NexGen is a well-structured learning project with five independently buildable Spring Boot services, Redis caching, Kafka event publishing, and a promising observability stack. It is **not** a production-ready e-commerce system.

The highest-risk facts are:

1. **Four of the five services accept unauthenticated, unauthorized writes.** Product, cart, order, and inventory expose CRUD/checkout endpoints with no usable authentication; inventory has Spring Security on the classpath but no security configuration, and the Kong JWT plugin is not connected to application-issued JWTs.
2. **Any caller can register as an administrator.** `UserService.registerUser` copies the client-supplied `role` field directly into the persisted user.
3. **Password hashes are returned by the API and stored in Redis.** `/api/v1/users/register` and `/api/v1/users/me` serialize the JPA `User` entity, including its BCrypt hash.
4. **Access and refresh tokens are interchangeable.** Both are signed with the same key and have no token-type claim. A 7-day refresh token is accepted as a bearer access token; a 24-hour access token is accepted by the refresh endpoint.
5. **Checkout accepts client-controlled prices, reports a fake payment success, deletes the cart before an order exists, and never reserves inventory.** Order creation accepts client-controlled user IDs, quantities, and prices with no idempotency key.
6. **Every cart and order endpoint is vulnerable to horizontal privilege escalation (IDOR)** because the user identity is taken from the URL path or request body rather than from the authenticated principal.
7. **The documented deployment cannot work as written.** At audit time there were no Dockerfiles, no database initialization scripts, no `.env.example`, and no CI. The Kong bootstrap script calls files that do not exist. SQL Server databases are never created. Kafka is plaintext, contradicting the README's SASL_SSL claim. Batch 1 adds `.env.example`; the rest remain open.
8. **The test and quality gates are not release-grade.** There is no CI, no integration test profile, no Testcontainers, no security tests, and several tests already fail.

**Current working-tree update (2026-10-26):** Items 2, 3, and 4 are closed by Batch 1. Item 1 and item 6 are closed at the application/service layer by Batch 2, but not at the deployment/Kong layer. Item 5 is only partially addressed: order `userId` is now token-derived, while prices, quantities, stock, payment, idempotency, and cart deletion ordering remain open. Item 7 remains open except for `.env.example`. Item 8 improved (161 tests, 156 passing, 5 skipped) but still lacks CI and integration tests. The detailed resolution notes are attached to each finding.

### 2.1 Severity summary

| Severity | Count | Meaning |
|---|---:|---|
| Critical | 11 | Direct compromise, fraud, data loss, or complete deployment blocker |
| High | 14 | Serious security/correctness/reliability issue; must be fixed before production |
| Medium | 12 | Important hardening, operational, or maintainability issue |
| Low | 5 | Documentation, hygiene, or defense-in-depth |

The detailed findings are in Section 5. The remediation sequence is in `docs/IMPLEMENTATION_ROADMAP.md`.

---

## 3. Service inventory (as built)

| Service | Port | Datastore | Cache | Security on classpath | Effective auth | Kafka | Actuator |
|---|---:|---|---|---|---|---|---|
| user-service | 8081 | SQL Server `userdb` | Redis | Spring Security + JWT | JWT on `/api/v1/users/**` except register/login/refresh | producer + demo consumer | **missing dependency** |
| inventory-service | 8082 | SQL Server `inventorydb` | Redis | Spring Security (no config) | Spring Boot default basic auth only; API effectively unusable/ambiguous | producer | present |
| product-service | 8083 | SQL Server `productdb` | Redis | none | **none** | producer | present |
| order-service | 8084 | SQL Server `orderdb` | Redis | none | **none** | producer | present |
| cart-service | 8086 | SQL Server `cartdb` | Redis | none | **none** | producer + Redis retry queue | present |

Infrastructure in `docker-compose.yml`: SQL Server 2022, Redis 7, Zookeeper, Kafka (Confluent 7.4), Kafka UI, Kong 3.4 + Kong migrations, Prometheus, Grafana, Loki, Promtail. **No application containers and no Dockerfiles exist.**

### 3.1 Actual runtime topology

```
Browser/curl ──► Kong :8000 ──► (routes defined by broken scripts)
                    │
                    └── direct access also possible:
                        :8081 user, :8082 inventory, :8083 product,
                        :8084 order, :8086 cart
                              │
        ┌─────────────────────┼──────────────────────┐
        ▼                     ▼                      ▼
   SQL Server :1433       Redis :6379           Kafka :9092
   (5 databases,          (no auth)             (PLAINTEXT,
    never created)                               auto-create)
```

There is **no service-to-service HTTP or Kafka consumer path** that implements checkout, stock reservation, payment, or notification. The only consumer is a demo `@KafkaListener` in `user-service` test sources that logs events.

---

## 4. Confirmed build and test baseline

### 4.1 Builds

All five modules package successfully with tests skipped:

```
user-service        BUILD SUCCESS
product-service     BUILD SUCCESS
inventory-service   BUILD SUCCESS
cart-service        BUILD SUCCESS
order-service       BUILD SUCCESS
```

### 4.2 Test detail

| Module | Test class | Result | Root cause |
|---|---|---|---|
| user | `UserServiceTest` | 7 pass | — |
| product | `ProductControllerTest` | 6 pass | — |
| product | `KafkaProducerServiceTest` | 3 pass | — |
| product | `ProductServiceTest` | 2 errors | `ProductMetrics` is not mocked; field is null under `@InjectMocks` |
| inventory | `InventoryControllerTest` | 9 pass | passes only because `@WithMockUser` supplies a principal |
| inventory | `InventoryServiceImplTest` | 6 pass | — |
| inventory | `KafkaInventoryProducerTest` | 1 pass | — |
| cart | `CartServiceImplTest` | 4 failures, 1 error | tests still expect the service to publish events; service no longer does; response/status assumptions drifted |
| order | `OrderServiceImplTest` | 1 failure, 2 errors | `MeterRegistry` is null; invalid-transition test receives NPE instead of `IllegalStateException` |

`@SpringBootTest` classes (`*ApplicationTests`, `AuthControllerTest`, `UserControllerTest`, `CartControllerTest`, `OrderControllerTest`, `JwtServiceTest`, `RedisServiceTest`) were **not** completed. They block on SQL Server/Redis/Kafka. No H2 or Testcontainers profile exists.

### 4.3 Test-quality observations

- Several tests assert behavior that the implementation no longer has (cart event publishing, unwrapped response bodies, `204` on clear).
- No test covers authorization, ownership, token-type confusion, refresh reuse, password-hash exposure, price tampering, duplicate orders, concurrency, or migration safety.
- No test starts the application with production-like configuration.
- No CI workflow runs any of these tests.

---

## 5. Findings

Severity is assigned from the perspective of a production e-commerce deployment. "Exploit" describes what an attacker or faulty client can do once they can reach the service; it does not imply that a live exploit was executed during this audit.

### 5.1 Critical

#### C-01 — Product, inventory, cart, and order APIs have no usable authentication or authorization

**Evidence**
- `product-service/.../controller/ProductController.java`, `cart-service/.../controller/CartController.java`, and `order-service/.../controller/OrderController.java` expose create/update/delete/checkout/status endpoints without `@PreAuthorize` or `@Secured`.
- `product-service/pom.xml`, `cart-service/pom.xml`, and `order-service/pom.xml` do not depend on `spring-boot-starter-security`.
- `inventory-service/pom.xml` depends on Spring Security, but the service has no `SecurityConfig`. Spring Boot therefore falls back to generated HTTP Basic credentials, which is not an authorization model and does not accept the application's JWT.
- `user-service/.../config/SecurityConfig.java:32-36` protects only the user service. It does not protect the other services.
- `docker-compose.yml` publishes infrastructure ports but does not containerize the services; the services are expected to run on host ports 8081–8086. Kong is therefore optional, and direct access bypasses it.
- `kong-automation/services/setup-*-service.sh` applies the Kong `jwt` plugin at the **service** level, so it also protects `/api/v1/users/login` and `/api/v1/users/register`, making login through Kong impossible with the scripts as written.

**Impact / exploit**
- `curl -X DELETE http://host:8083/api/v1/products/{sku}` destroys catalog data.
- `curl -X POST http://host:8084/api/v1/orders` creates arbitrary orders.
- `curl -X PUT http://host:8082/api/v1/inventory/{sku}` rewrites stock.
- Any cart can be read or emptied by calling `/api/v1/cart/{userId}`.
- The gateway provides no defense because it is not in the request path for direct service calls and its JWT credentials are unrelated to application tokens.

**Required fix**
1. Add Spring Security to every service and a shared resource-server configuration that validates the same issuer and signing key as the user service.
2. Deny by default; explicitly allow only health/info and the public auth endpoints.
3. Enforce roles per operation (catalog and inventory writes: admin; cart/order: authenticated user; order status transitions: admin or internal service).
4. Remove published service ports in deployed environments; route all external traffic through Kong or an ingress.
5. Make Kong JWT validation use the application's key/issuer, or validate tokens in the services and use Kong only for edge concerns.
6. Add authorization integration tests that assert 401/403 for each protected route.

**Verification**
- Every non-public endpoint returns 401 without a token.
- A user token cannot perform admin writes (403).
- A direct request to a service port is impossible in the deployed network policy.
- No route lets an unauthenticated caller reach a write or checkout operation.

**Resolution (Batch 2, partial):** Product, inventory, cart, and order now depend on Spring Security and OAuth2 resource-server support. Each validates the user-service HS256 `JWT_SECRET`, issuer, audience, signature, expiry, and `token_type=access`; refresh tokens are rejected. Security is deny-by-default with explicit public product reads plus Swagger/Actuator health/info/error paths. Product writes require `ROLE_ADMIN`; inventory reads require authentication and writes require `ROLE_ADMIN`/`ROLE_INTERNAL`; cart and order ownership is enforced; order status updates are admin/internal-only. JSON 401/403 responses are consistent, and real signed-token tests cover missing, refresh, wrong-audience, wrong-issuer, role, and owner cases in all four services.

**Still open for C-01:** Service ports remain direct and Kong is still broken/unvalidated; there is no deployed network policy. Swagger/OpenAPI is public. HS256 with a shared secret remains instead of RS256/JWKS. No admin/internal provisioning path exists. Runtime verification through Compose/Kong was not performed.

---

#### C-02 — Any caller can register as an administrator (mass assignment)

**Evidence**
- `user-service/.../service/UserService.java:32`:
  `.role(request.getRole() != null ? request.getRole() : "ROLE_USER")`
- `UserRegistrationRequest` contains a client-writable `role` field.
- `UserController.register` at lines 25-28 uses `@RequestBody` without effective validation.

**Impact / exploit**
`POST /api/v1/users/register` with `{"username":"x","password":"...","role":"ROLE_ADMIN"}` persists `ROLE_ADMIN`. Even though there are few admin endpoints today, `SecurityConfig` already has a `/api/admin/**` rule, and any future role check is compromised.

**Required fix**
- Remove `role` from the public registration contract (or reject requests containing it).
- Always persist `ROLE_USER` on self-registration.
- Provide a separate, audited admin-provisioning path (CLI/bootstrap with a one-time secret, not a public API).
- Add a test proving a supplied role is ignored/rejected.

**Verification**
- Registration response and persisted row are `ROLE_USER` regardless of request.
- A test asserting an attempted `ROLE_ADMIN` registration cannot obtain admin authority.

**Resolution (Batch 1):** `UserRegistrationRequest` no longer has a `role` property; it also declares `@JsonIgnoreProperties("role")` to ignore the legacy field explicitly. `UserService.registerUser` always persists `ROLE_USER`. `UserControllerTest.registrationReturnsProfileWithoutPasswordAndIgnoresClientRole` and `RequestValidationTest.clientSuppliedRoleIsNotPartOfTheRegistrationContract` pass in the isolated suite. A separate admin-provisioning path is still not implemented; admin accounts must be seeded out-of-band.

---

#### C-03 — Password hashes are returned by the API and cached

**Evidence**
- `user-service/.../controller/UserController.java:26-27` returns `ResponseEntity<User>` from `/register`.
- `UserController.java:45-49` returns `ResponseEntity<User>` from `/me`.
- `User` is a JPA entity with a `password` field and Lombok `@Data`/builder accessors.
- `UserService.java:68-72` annotated the entity-returning lookup with `@Cacheable(value = "users", ...)`. The user service does not pin `spring.cache.type`, so the hash could be serialized into Redis or an in-process cache depending on auto-configuration.

**Impact / exploit**
- Any client that registers or calls `/me` receives the BCrypt hash. Hashes can be cracked offline, and a leaked hash enables credential stuffing against other systems.
- The hash is also copied into Redis and any cache dump/backup.

**Required fix**
- Introduce `UserProfileResponse` (id, username, email, role, createdAt) and return it from all user-facing endpoints.
- Add `@JsonIgnore` to `User.password` as defense-in-depth.
- Cache only the response DTO, or evict/avoid caching sensitive entities.
- If this code has been deployed, treat hashes as exposed and force password resets.

**Verification**
- `/register` and `/me` JSON never contain `password`.
- Redis values for the user cache contain no hash.
- Tests assert the serialized contract.

**Resolution (Batch 1):** `/register` and `/me` now return `UserProfileResponse`; `User.password` is annotated `@JsonIgnore` as defense-in-depth; and user-profile caching was removed, so no profile or password hash is cached. `RequestValidationTest.profileSerializationContainsNoPassword` and the controller tests assert the JSON contract. If this code was deployed before Batch 1, previously exposed hashes must still be treated as compromised.

---

#### C-04 — Access and refresh tokens are interchangeable, untyped, and hardcoded

**Evidence**
- `JwtService.java:39-45` builds access tokens with only `sub`, `iat`, `exp` and a hardcoded 24-hour lifetime.
- `JwtService.java:61-67` builds refresh tokens with the same key, same claims, and a hardcoded 7-day lifetime.
- Neither token has a `typ`, `iss`, `aud`, or `jti` claim.
- `JwtAuthFilter.java:53-54` accepts any token that parses and is valid, so a refresh token authenticates API calls.
- `AuthController.java:81-98` accepts any valid token as a refresh token, including an access token.
- At baseline, `application.yml:36-38` defined `jwt.expiration: 3600000`, but `JwtService` never read it.
- `Keys.hmacShaKeyFor(secretKey.getBytes())` is called lazily; a short secret fails on first token operation rather than at startup, surfacing as a 500.

**Impact / exploit**
- A stolen refresh token grants 7 days of full API access and can mint unlimited new tokens.
- A stolen access token can be used at the refresh endpoint for its full 24-hour lifetime.
- There is no way to distinguish token classes, revoke a family, or rotate keys safely.
- The intended 1-hour expiry is not applied.

**Required fix**
- Add a `token_type` (`access`/`refresh`) claim. Reject refresh tokens in the authentication filter and access tokens at the refresh endpoint.
- Add `iss` and `aud`; validate both on parse.
- Add `jti` and a session/family identifier for revocation and reuse detection.
- Bind lifetimes to configuration (`jwt.access-ttl`, `jwt.refresh-ttl`), defaulting to 15 minutes / 7 days.
- Validate the secret at startup (≥32 bytes for HS256) and fail fast with a clear error.
- Prefer asymmetric signing (RS256/ES256) with a JWKS endpoint for multi-service verification.

**Verification**
- Refresh token on `/me` → 401; access token on `/refresh-token` → 401.
- Expired tokens → 401, not 500.
- Short secret prevents startup.
- Unit tests cover claims, type checks, expiry, and key length.

**Resolution (Batch 1):** Access and refresh tokens now carry `token_type`, `iss`, `aud`, `jti`, and roles; lifetimes come from `JWT_ACCESS_TTL_MS` (default 15 minutes) and `JWT_REFRESH_TTL_MS` (default 7 days); `JwtService` validates the key at startup (minimum 32 bytes) and rejects an access TTL that is not shorter than the refresh TTL. The filter accepts only access tokens and the refresh endpoint rejects access tokens. `JwtServiceTest` covers claims, TTLs, expiry, wrong key/issuer/audience, and weak-secret startup failure; `JwtAuthFilterTest` and `AuthControllerTest` cover the endpoint behavior. Asymmetric signing/JWKS is still future work.

---

#### C-05 — Cart and order endpoints are vulnerable to horizontal privilege escalation (IDOR)

**Evidence**
- `CartController` maps `/api/v1/cart/{userId}`, `/api/v1/cart/{userId}/add`, `/api/v1/cart/{userId}/remove`, `/api/v1/cart/{userId}/clear`.
- `OrderController` maps `/api/v1/orders/user/{userId}`, `/api/v1/orders/{orderNumber}`, and cancel/status operations.
- The path/body identity is trusted directly; no method compares it with `Authentication`/`Principal`.
- The services have no authentication (C-01), so the path value is the only identity.

**Impact / exploit**
- Change another user's cart by substituting `{userId}` (likely the username, which is enumerable).
- Read order history for any user.
- Cancel or mutate any order if the order number is known or leaked (order numbers are random UUIDs, but they appear in logs, events, and support tooling).

**Required fix**
- Derive the acting user from the authenticated principal, not from the URL/body.
- Remove `{userId}` from user-facing routes or verify it equals the principal.
- Constrain repository queries by owner ID; do not fetch-then-check in a way that leaks existence.
- Allow admin/internal overrides only with explicit roles and audit logging.
- Add tests where user A tries to access user B's cart/order and receives 403/404.

**Verification**
- Cross-user requests fail for every cart/order route.
- Repository queries include the owner predicate.

**Resolution (Batch 2, partial):** Cart endpoints compare the path `userId` with `Authentication.getName()` and reject mismatches with 403; checkout also requires the body `userId` to match the principal. Order creation derives `userId` from the token and no longer accepts it from the client. Order reads, cancellation, and history verify ownership in the service layer and return 404 for non-owners; listing is owner-only unless the caller is admin/internal. Status changes require admin/internal. `@WebMvcTest` cases cover cross-user cart access, cross-user order listing, owner-denial at the service boundary, and real-token role checks.

**Still open for C-05:** Ownership is enforced after fetching the row, not by repository predicates. Admin/internal overrides are not audit-logged. The `/api/v1/cart/{userId}` routes remain in the contract (now validated) and should be simplified to principal-only routes in a later batch. Runtime cross-user tests against a live stack were not performed.

---

#### C-06 — Cart checkout trusts client prices, fakes payment success, and destroys the cart before an order exists

**Evidence**
- `CartServiceImpl.java:84` and `:90` set the cart line price from `itemRequest.getPrice()`.
- `CartServiceImpl.java:173-175` computes the total from those stored client prices.
- `CartServiceImpl.java:185` sets `paymentStatus("SUCCESS") // Simulated for now`.
- `CartServiceImpl.java:181` deletes all cart items before any order is created; there is no order-service call and no inventory reservation.
- `CartController.clearCart` and `checkout` call `getItemsByUserId` **after** deletion, so emitted events contain empty product IDs and a zero total.
- `CartServiceImpl.java:228-234` returns a `FAILED` checkout response with HTTP 200 via a fallback, hiding the failure.

**Impact / exploit**
- Checkout any product at an arbitrary price, including zero or negative.
- Receive a success confirmation without payment, stock deduction, or an order.
- Lose the cart contents if anything fails after deletion.
- Downstream analytics and events are wrong.

**Required fix**
- Fetch authoritative product price and stock from the product/inventory service; never accept price from the client.
- Create the order (or start a saga) before clearing the cart; clear only after confirmation.
- Remove the simulated payment success. Until a real payment integration exists, return an explicit "not implemented" state or keep payment out of scope.
- Make checkout idempotent with a client-supplied idempotency key.
- Return proper 4xx/5xx errors instead of a 200 fallback.
- Add tests for price tampering, empty cart, downstream failure, and retry.

**Verification**
- Tampered price is ignored.
- A failed downstream call leaves the cart intact and creates no order.
- No response reports payment success without a payment provider reference.
- Duplicate checkout with the same key creates one order.

---

#### C-07 — Order creation trusts client identity, items, prices, and has no idempotency

**Evidence**
- `OrderServiceImpl.java:43-70`:
  - `order.setUserId(orderRequest.getUserId())` (client-controlled).
  - Item SKU, quantity, and price are copied from the request at lines 54-56.
  - Order number is a random UUID; there is no idempotency key.
  - No product/inventory validation and no total calculation.
  - Event publication happens after save, asynchronously, with no outbox.
- `OrderRequest` / `OrderItemRequest` carry the same client-controlled fields.

**Impact / exploit**
- Create orders for other users, with zero/negative quantities or arbitrary prices.
- Duplicate orders on client retries or network timeouts.
- Orders can reference nonexistent SKUs.
- Event loss leaves the system inconsistent.

**Required fix**
- Derive `userId` from the token.
- Resolve price and availability server-side; compute totals with `BigDecimal`.
- Validate SKU existence and positive quantities.
- Require an idempotency key with a unique constraint and return the original result on replay.
- Publish events through a transactional outbox.
- Add a state machine guard for all status transitions.

**Verification**
- Forged user/price/quantity is rejected or ignored.
- Replaying the same idempotency key returns the same order.
- Invalid SKU or insufficient stock returns 4xx.
- Event publication is atomic with the order transaction (or recoverable).

**Resolution (Batch 2, partial):** `OrderRequest` no longer contains `userId`; `OrderController` passes `Authentication.getName()` to the service, and `OrderServiceImpl` persists that value. Clients that still send `userId` cannot override the token subject. Order reads, cancellation, and history are owner-checked.

**Still open for C-07:** Order item SKU, quantity, and price are still client-supplied and unvalidated; there is no server-side price/stock lookup or total computation, no idempotency key, no state-machine guard, and no transactional outbox. These remain Batch 3 scope.

---

#### C-08 — The documented deployment cannot work from a clean clone

**Evidence**
- No `Dockerfile` exists anywhere in the repository.
- `docker-compose.yml` defines only infrastructure; the five applications are run manually in IntelliJ per README line 160.
- No SQL initialization or migration scripts exist. SQL Server starts with `master` only; `userdb`, `productdb`, `inventorydb`, `orderdb`, and `cartdb` are never created. Every application's `ddl-auto: update` needs an existing database.
- `kong-automation/setup-all.sh:5-10` calls `setup-user.sh`, `setup-cart.sh`, `setup-order.sh`, `setup-product.sh`, `setup-inventory.sh`, but the files are named `setup-user-service.sh`, etc. The bootstrap fails immediately.
- At baseline, `.env` was git-ignored and no `.env.example` existed. Compose references `${MSSQL_SA_PASSWORD}`, `${KONG_PG_PASSWORD}`, and other variables; without them the stack cannot start.
- The Kong JWT plugin is attached to the whole user service, so login/register are blocked through the gateway.
- README's `docker-compose up -d` starts infrastructure only, not the application.

**Impact**
- A new contributor or reviewer cannot reproduce the system.
- No production or staging deployment is possible.
- Any claim of "Dockerized microservices" is inaccurate.

**Required fix**
- Add multi-stage Dockerfiles per service (non-root user, JRE 21, healthcheck, read-only FS where possible).
- Add application services and profiles to Compose, or a separate `compose.app.yml`.
- Add an idempotent database/init job and Flyway migrations.
- Add `.env.example` with every required variable and safe placeholders; document secret injection.
- Fix Kong scripts (filenames, route-level plugins, public auth routes, consumers, health checks).
- Add a `Makefile`/scripts for build, up, migrate, smoke test, and down.
- Verify from a clean clone on a machine with Docker.

**Verification**
- `docker compose up` on a clean clone reaches healthy services.
- Login, product CRUD, cart, and order smoke tests pass through Kong.
- `docker compose down -v && docker compose up` is repeatable.

---

#### C-09 — Infrastructure is exposed with default/absent authentication and plaintext transport

**Evidence**
- `docker-compose.yml` publishes SQL Server (`6-7`), Redis (`17-18`), Kafka (`32-33`), Kong admin (`84`), Kafka UI (`92`), Prometheus (`104`), Grafana (`114`), and Loki (`136`) on all host interfaces by default.
- Redis has no password or TLS.
- Kafka listeners are `PLAINTEXT` (`39-42`), with auto-topic creation enabled (`43`), directly contradicting README line 12 and lines 111/113.
- Grafana uses `admin`/`admin` (`120-121`).
- Kong admin API is exposed on `8001` without authentication.
- Kong CORS is configured with `config.origins=*` and `config.credentials=true` in every service script, which is both unsafe and invalid for credentialed CORS.
- The SQL Server SA account is used by all services.

**Impact**
- Anyone who can reach the host can read/write all data, reconfigure Kong, or access Kafka UI.
- A compromised Redis can be used for cache poisoning; combined with permissive Jackson typing (H-13), it can become a deserialization attack surface.
- Plaintext Kafka allows message interception/tampering on the network.

**Required fix**
- Bind dev ports to `127.0.0.1`; remove public ports in deployed environments.
- Enable Redis AUTH/TLS; enable Kafka SASL_SSL and least-privilege ACLs; disable auto-topic creation.
- Use dedicated DB users per service with least privilege; never use `sa` at runtime.
- Put Kong admin on a private network with RBAC and no public port.
- Replace default Grafana credentials; disable anonymous access.
- Replace wildcard CORS with an explicit allowlist and no credentials unless required.
- Add network policies/security groups and TLS termination.

**Verification**
- External port scan shows only the edge proxy.
- Redis/Kafka/SQL reject unauthenticated connections.
- Kong admin is unreachable externally.
- CORS preflight from a disallowed origin fails.

---

#### C-10 — No schema migrations; `ddl-auto: update` is the only schema mechanism

**Evidence**
- All five configurations use `spring.jpa.hibernate.ddl-auto: update` (for example `user-service/.../application.yml:16`, `product-service/.../application.yaml:16`).
- No Flyway/Liquibase dependency or migration directory exists.
- No `*.sql` files exist in the repository.
- `show-sql: true` and Hibernate SQL DEBUG are enabled in every service.

**Impact**
- Schema changes are applied implicitly at startup with no review, ordering, or rollback.
- Concurrent deployments can race and corrupt schema.
- There is no reproducible database state for tests or production.
- Fresh environments fail because the databases themselves are not created.

**Required fix**
- Add Flyway (or Liquibase) per service with versioned migrations.
- Create databases in an init job with explicit collation/options.
- Switch runtime to `ddl-auto: validate`.
- Separate migration credentials from runtime credentials.
- Disable SQL logging in production; use parameterized logging if needed.

**Verification**
- A clean database migrates to the expected schema.
- Startup fails if the schema does not match.
- Migration tests run in CI against a disposable SQL Server container.

---

#### C-11 — Secrets have unsafe defaults and no documented contract

**Evidence**
- `user-service/.../application.yml:11` and `product-service/.../application.yaml:11` default the DB password to `YourStrong@Passw0rd`.
- `user-service/.../application.yml:37` requires `${JWT_SECRET}` with no default, but the README never mentions it; a fresh start fails with an unresolved placeholder.
- At baseline, `.env` was ignored but no `.env.example` was committed, while Compose requires several variables.
- Grafana defaults are hardcoded (`docker-compose.yml:120-121`).
- Secrets are passed as environment variables without a secret manager.
- The database URL uses `trustServerCertificate=true`, disabling certificate validation.

**Impact**
- Default credentials are copied into deployments.
- Developers are forced to invent ad-hoc secrets, which end up in shell history or local files.
- Startup failures are confusing and undocumented.
- Database traffic is vulnerable to MITM because the certificate is not validated.

**Required fix**
- Remove all secret defaults; fail fast with a clear message listing missing variables.
- Add `.env.example` with placeholders and a documented variable table.
- Use a secret manager (Azure Key Vault, AWS Secrets Manager, Vault) in deployed environments.
- Rotate any credential that may have been exposed in git history.
- Enable proper TLS trust (`encrypt=true;trustServerCertificate=false` with a trusted CA).

**Verification**
- Startup without secrets fails with an actionable error.
- `git grep` finds no real credentials.
- TLS connection validates the server certificate.

**Resolution (Batch 1, partial):** A root `.env.example` now documents every required variable with placeholders, and `JwtService` fails fast when `JWT_SECRET` is missing or shorter than 32 bytes. The hardcoded database password fallback and `trustServerCertificate=true` remain in the service configs; they must be removed in Batch 4/6, and any previously exposed credential must be rotated.

---

### 5.2 High

#### H-01 — Malformed, expired, or tampered JWTs produce HTTP 500 instead of 401

**Evidence:** `JwtAuthFilter.java:53-54` calls `jwtService.extractUsername(jwt)` outside any `try/catch`. `extractAllClaims` (`JwtService.java:31-37`) lets `ExpiredJwtException`, `MalformedJwtException`, `SignatureException`, and `UnsupportedJwtException` escape. `GlobalExceptionHandler.java:40-43` converts them to `500 Internal error: ...`.

**Impact:** A client with a stale token sees a server error and may retry indefinitely; error messages can leak parsing details. Monitoring cannot distinguish bad credentials from server faults.

**Fix:** Catch `JwtException` and `IllegalArgumentException` in the filter, clear the security context, and return a JSON 401. Configure an `AuthenticationEntryPoint` and `AccessDeniedHandler` so all auth failures are consistent. Never include exception text in the response.

**Verification:** `Authorization: Bearer garbage`, an expired token, and a token signed with another key all return 401 with a stable error code.

**Resolution (Batch 1):** `JwtAuthFilter` catches `JwtException`/`IllegalArgumentException` and returns a JSON 401 with `INVALID_TOKEN`; Redis failures return 503 `AUTH_UNAVAILABLE`; `SecurityConfig` installs JSON `AuthenticationEntryPoint` and `AccessDeniedHandler`. `JwtAuthFilterTest` covers malformed, expired, tampered, revoked, and refresh-token bearer cases. No exception text is returned.

---

#### H-02 — Refresh rotation has no reuse detection, and logout/password change do not revoke sessions

**Evidence:**
- `AuthController.java:81-98` reads the stored refresh token, compares it, and overwrites it. There is no family ID, no reuse detection, and no atomic compare-and-delete; concurrent refreshes can both succeed.
- `AuthController.java:68-79` blacklists only the access token and never deletes `refresh_token:<username>`.
- `SecurityConfig.java:33` marks `/logout` and `/refresh-token` as `permitAll`, so logout does not require authentication.
- `RedisService.java:17-20` stores one refresh token per username (no multi-device sessions) for a fixed 7 days.
- `UserService.changeUserPassword` (`:55-66`) does not delete the refresh token or blacklist anything.
- `LogoutService.java:15-21` stores the raw JWT as a Redis key on every request check; a Redis outage breaks authentication.

**Impact:** A stolen refresh token survives logout and password changes for up to 7 days. Replaying a rotated token is not detected. Concurrent refresh can produce multiple valid tokens. Users cannot revoke a compromised session.

**Fix:** Implement per-session refresh records (`jti`/family ID), atomic consume-and-rotate (`GETDEL`/Lua), reuse detection that revokes the family, delete refresh state on logout and password change, require authentication for logout, and add a token version/`token_not_before` claim if global revocation is needed. Hash refresh tokens at rest. Define Redis failure behavior (fail closed with 503, not 500). Add multi-device support or explicitly document single-session behavior.

**Verification:** Old refresh token after rotation → 401 and family revoked; logout then refresh → 401; password change then refresh → 401; parallel refresh only one succeeds; Redis outage returns 503 with a clear error.

**Resolution (Batch 1):** Refresh tokens are stored as SHA-256 hashes with the configured TTL. A Lua script atomically compares the presented hash, replaces it, and records the used hash; exactly one caller wins. Replaying a used token revokes the current session, while an unknown token is rejected without deleting valid state (preventing an unauthenticated logout DoS). Logout now requires authentication and deletes refresh state as well as blacklisting the access token; password change deletes refresh state. `RedisServiceTest` covers hashing, rotation results, replay mapping, and deletion; `AuthControllerTest` covers rotation, replay, unknown-token, and logout behavior. The design remains one refresh session per username; per-device/family records and global token-version revocation are not implemented.

---

#### H-03 — Bean Validation is not enforced in user, product, and inventory; order DTOs have no constraints

**Evidence:**
- `user-service/pom.xml` and `product-service/pom.xml` depend only on `jakarta.validation-api` (confirmed by `dependency:tree`), not on `spring-boot-starter-validation`. Without a provider (Hibernate Validator), `@Valid` does nothing.
- `UserController` and `ProductController` use `@RequestBody` without `@Valid`; even if added, no provider exists.
- `order-service` and `cart-service` include the validation starter, but `OrderRequest`, `OrderItemRequest`, and several cart DTOs have no constraints.
- No controller validates path variables or pagination parameters.
- `GlobalExceptionHandler` has a `MethodArgumentNotValidException` handler, but user/product never trigger it.

**Impact:** Empty usernames, weak/oversized passwords, invalid emails, negative quantities, negative/zero prices, huge page sizes, and malformed SKUs reach the database. Failures surface as 500s or corrupt rows instead of 400s.

**Fix:** Add the validation starter to every service; annotate DTOs (`@NotBlank`, `@Size`, `@Email`, `@Positive`, `@PositiveOrZero`, `@Pattern`, `@Min/@Max`); add `@Valid` to request bodies; validate `@PathVariable` with `@Validated`; cap pagination; add tests for each constraint.

**Verification:** Invalid payloads return 400 with field-level errors; valid boundary values succeed; the app fails to start if validation is misconfigured? (No — test via MockMvc.)

**Resolution (Batch 1, user portion):** `spring-boot-starter-validation` is present; registration, login, refresh, profile-update, and password-change DTOs have constraints; controllers use `@Valid`; `GlobalExceptionHandler` returns field-level `VALIDATION_ERROR` responses. `RequestValidationTest`, `UserControllerTest`, and `AuthControllerTest` cover the constraints. Product, inventory, cart, and order validation remain open for later batches.

**Batch 2 note:** Product entity fields already carry constraints and the controller uses `@Valid`; cart DTOs have basic constraints. `OrderItemRequest` still has no constraints, inventory entities are not validated, and pagination/path parameters are not capped. Batch 2 did not close H-03.

---

#### H-04 — CORS is wide open and invalid for credentialed requests

**Evidence:**
- `AuthController.java:29` and `UserController.java:20` use bare `@CrossOrigin()`, which allows all origins, methods, and headers.
- `InventoryController` also uses `@CrossOrigin` (see controller annotations).
- Every Kong service script sets `config.origins=*` together with `config.credentials=true` (`kong-automation/services/setup-*-service.sh`). Browsers reject wildcard origins with credentials; Kong may also reject the combination.

**Impact:** Any website can issue credentialed requests to the API if a token is stored in a cookie (currently tokens are returned in JSON, but this becomes dangerous if cookie storage is added). The Kong configuration is internally inconsistent and may fail to load.

**Fix:** Remove controller-level `@CrossOrigin`; define one `CorsConfigurationSource` with an explicit allowlist from configuration, allowed methods/headers, and `allowCredentials` only when necessary. Configure Kong CORS to match; do not use `*` with credentials.

**Verification:** Preflight from an allowed origin succeeds; a disallowed origin is rejected; Kong config validates.

**Resolution (Batch 1, user portion):** Controller-level `@CrossOrigin` annotations were removed from user-service. `SecurityConfig` builds a `CorsConfigurationSource` from `CORS_ALLOWED_ORIGINS` (comma-separated), with explicit methods/headers and `allowCredentials=false`. `CorsConfigTest` verifies allowed origins and rejects an unlisted origin. Kong CORS remains open and is not validated.

**Resolution (Batch 2, service portion):** `@CrossOrigin` was removed from inventory and no `@CrossOrigin` remains anywhere in `nexgen-server`. Product, inventory, cart, and order each build a `CorsConfigurationSource` from their own `CORS_ALLOWED_ORIGINS`, allow only GET/POST/PUT/DELETE/PATCH/OPTIONS, and set `allowCredentials=false`. Kong CORS is still unvalidated; the service-level finding is closed for the application code.

---

#### H-05 — Kafka eventing is not reliable and the Saga is not implemented

**Evidence:**
- Producers call `kafkaTemplate.send(...)` and ignore the returned `CompletableFuture` (for example `KafkaProducerService.java:30`); failures are not observed or retried.
- Order events are sent after `save` with no outbox; a crash between DB commit and send loses the event.
- `CartServiceImpl` used to publish events but no longer does; the controller publishes some events after the service call, outside the transaction.
- There are no `@KafkaListener` consumers in inventory, order, cart, or payment (no payment service exists). The only consumer is a demo in user-service test sources.
- `KafkaRetryScheduler` (cart) removes entries from the Redis list even when the send fails and processes the queue serially, causing head-of-line blocking.
- No event ID, schema version, partition key strategy for ordering, DLQ, idempotent consumer, or duplicate suppression exists.
- `spring.json.trusted.packages: '*'` is configured in product/cart/order.
- README claims a choreographed Saga with inventory, payment, and notification; none of it exists.

**Impact:** Orders can be accepted without stock reservation or payment; events can be silently lost; retries can duplicate side effects; the system cannot recover to a consistent state.

**Fix:** Define an event envelope (`eventId`, `eventType`, `version`, `occurredAt`, `producer`, `correlationId`, `payload`), use a transactional outbox, publish with keys and acknowledgements, add DLQ/retry topics, make consumers idempotent, and implement the order/inventory/payment saga explicitly (or remove the claim). Never fabricate success.

**Verification:** Broker-down test leaves the outbox row and later publishes; duplicate delivery produces one effect; poison message lands in DLQ; saga compensation test passes.

---

#### H-06 — Fallbacks fabricate success or silently discard work

**Evidence:**
- `CartServiceImpl.fallbackCheckout` returns a 200 response with `paymentStatus("FAILED")` and zero totals instead of an error.
- `KafkaFallbackService` enqueues failed events to Redis, but `KafkaRetryScheduler` removes each entry before/regardless of send success (see `KafkaRetryScheduler`).
- Cart `clearCart`/`checkout` controller methods query items after deletion, producing empty events and zero totals.

**Impact:** Clients and upstream systems believe an operation succeeded when it did not. Failed events are lost. Operators cannot distinguish a real checkout from a fallback.

**Fix:** Return proper 5xx/4xx errors; remove fabricated success paths; make the retry queue acknowledge only after a confirmed send; persist failures with an attempt count and dead-letter after a bound; correct the read-after-delete ordering.

**Verification:** Downstream failure returns an error and leaves state unchanged; retry queue retains events until success or DLQ; clear/checkout events contain the correct items and totals.

---

#### H-07 — Cache keys and invalidation produce incorrect responses

**Evidence:**
- `OrderServiceImpl.java:85-88` caches `getOrdersByUserId` with key `userId:page:size` but omits the optional `status` filter. A request for `status=CREATED` can return a cached unfiltered or differently filtered page.
- Product `getAllProducts` cache (`product-service` `ProductServiceImpl`/`PageCacheUtil`) is not invalidated on create/update/delete in all paths.
- `UserService.getUserByUsername` caches the entity (`:68-72`), and `changeUserPassword` (`:55-66`) has no `@CacheEvict`, so `/me` can serve a stale user object (including the old password hash).
- Cache keys do not include tenant/authorization context where relevant.

**Impact:** Users see the wrong orders or stale product/user data after writes; a password change may appear not to take effect in cached responses; authorization decisions based on cached entities can be stale.

**Fix:** Include every query parameter in the key; use explicit eviction on all mutations; cache DTOs, not entities; add TTLs and metrics; add tests that mutate then read through the cache.

**Verification:** Changing a filter never returns another filter's page; create/update/delete invalidates list/detail caches; password change invalidates the user cache.

**Resolution (Batches 1–2, partial):** The user-profile cache was removed. `getOrdersByUserId` now includes the `status` filter in its cache key. The cached single-order `getOrderById` path was removed in Batch 2 so an owner check cannot be bypassed from cache. Product list/detail cache invalidation on every create/update/delete path is still unverified, and no test mutates then reads through the cache. Cart caching and TTL strategy remain unchanged.

---

#### H-08 — Monetary values use `double`

**Evidence:** `CartItem.price` is `double`; `CartServiceImpl` performs `double` arithmetic (`:84`, `:90`, `:122`, `:173-175`); order items and product prices use `Double`/`double`; DTOs mirror this.

**Impact:** Rounding errors, negative totals from subtraction, and inconsistent totals between cart and order. Financial values are not safe for billing or auditing.

**Fix:** Use `BigDecimal` with an explicit scale/rounding policy (for example scale 2, `HALF_UP`) in entities, DTOs, and calculations; add currency where multi-currency is intended; add property-based tests for arithmetic.

**Verification:** Monetary tests use exact decimal assertions; no `double` remains in money paths.

---

#### H-09 — Inventory has no reservation or atomic decrement; negative stock is possible

**Evidence:** `InventoryServiceImpl` exposes create/update/delete/get only. There is no reserve/release/confirm operation, no `@Version` optimistic lock, no conditional `UPDATE ... WHERE quantity >= ?`, and no check preventing negative quantities on update. Order/cart never call inventory.

**Impact:** Concurrent checkouts oversell stock; an operator can set negative stock; there is no way to compensate a failed order.

**Fix:** Add reservation records with expiry, atomic conditional decrement, optimistic locking, and release/confirm operations. Validate `quantity >= 0`. Integrate with the order saga. Add concurrency tests.

**Verification:** Two concurrent reservations for the last unit yield one success and one conflict; failed orders release stock; negative quantities are rejected.

---

#### H-10 — Missing database constraints and uniqueness guarantees

**Evidence:** No unique constraint on `Order.orderNumber`, `(CartItem.userId, productId)`, or user email. No check constraints for positive quantity/price. Registration checks username with a read-then-write, which races. Cart add does the same for an existing line.

**Impact:** Duplicate order numbers, duplicate cart lines, duplicate accounts, and invalid rows can be persisted under concurrency or direct DB access.

**Fix:** Add unique indexes and check constraints via migrations; handle `DataIntegrityViolationException`; use `INSERT ... ON CONFLICT`/retry or a transaction with proper isolation.

**Verification:** Concurrent duplicate creation yields one row and a 409 for the loser; DB rejects negative values.

---

#### H-11 — Observability is incomplete and logs are not production-safe

**Evidence:**
- `prometheus/prometheus.yml` scrapes only order (8084) and cart (8086); user, product, and inventory are absent.
- `user-service/pom.xml` has no Actuator dependency, despite `application.yml:44-59` configuring management endpoints.
- `logback-spring.xml` in every service writes only to `../../../logs/<service>/application.log` (relative to the process CWD), with no console appender, no rotation, and no size cap. `docker logs` will be empty; the file path breaks under containers.
- `promtail-config.yaml` watches only cart and Kong.
- No correlation ID, trace ID, request ID, or MDC propagation code exists.
- `management.endpoint.health.show-details: always` exposes component details publicly.
- No alert rules, SLOs, dashboards-as-code, or error budget exist.
- Hibernate SQL DEBUG and `show-sql: true` are enabled in all services.

**Impact:** Incidents are hard to diagnose; disks can fill; sensitive SQL and data can leak into logs; metrics miss 60% of services; containers produce no logs.

**Fix:** Add Actuator to all services; use a console + rolling file appender (or stdout only in containers); set sane levels; add request/correlation IDs; scrape all services; add Prometheus alert rules and Grafana provisioning; restrict health details; ship logs from stdout via the platform.

**Verification:** All five `/actuator/prometheus` targets are UP; logs appear in `docker logs`; correlation IDs span service calls; disk usage is bounded.

---

#### H-12 — No application-level rate limiting, brute-force protection, or account lockout

**Evidence:** Kong scripts apply a 10/minute rate limit at the gateway, but direct service ports bypass it. There is no login attempt counter, lockout, CAPTCHA, or per-IP/per-account throttling in the user service. Passwords have no policy (no constraints are enforced; see H-03).

**Impact:** Unlimited password guessing, credential stuffing, registration spam, and checkout abuse. The gateway limit is ineffective when services are reachable directly.

**Fix:** Add per-IP and per-account rate limits at the edge and in the auth service; implement exponential backoff/lockout with safe unlock; enforce a password policy; log and alert on repeated failures; ensure direct ports are not reachable.

**Verification:** Repeated failed logins are throttled/locked; limits survive restarts where required; direct-port access is blocked.

---

#### H-13 — Unsafe Jackson polymorphic typing and wildcard trusted packages

**Evidence:**
- `order-service/.../config/RedisConfig.java:39-43` uses `LaissezFaireSubTypeValidator.instance` with `DefaultTyping.NON_FINAL`.
- `product-service/.../config/RedisCacheConfig.java:37-40` uses `DefaultTyping.EVERYTHING` with the default (permissive) validator.
- `cart-service/.../config/RedisCacheConfig.java:38` and `inventory-service/.../config/RedisConfig.java:21-24` use `DefaultTyping.NON_FINAL` with the default validator.
- `spring.json.trusted.packages: '*'` in product/cart/order Kafka consumers.

**Impact:** If an attacker can write to Redis or Kafka, polymorphic deserialization can instantiate arbitrary classes (gadget chains), potentially leading to RCE. The wildcard trusted packages remove the deserialization type allowlist.

**Fix:** Remove default typing; serialize concrete DTOs with explicit types; if polymorphism is required, use a strict `PolymorphicTypeValidator` allowlist. Set `spring.json.trusted.packages` to the application packages only. Treat cache/broker data as untrusted.

**Verification:** Deserialization of a non-allowlisted type fails; Redis/Kafka data cannot instantiate arbitrary classes.

---

#### H-14 — Tests and CI are not release-grade

**Evidence:**
- No `.github/workflows` or other CI configuration exists.
- Full `mvn test` suites do not complete without SQL Server/Redis/Kafka; there is no H2/Testcontainers profile.
- Isolated tests already fail (product 2 errors, cart 5 issues, order 3 issues).
- No integration, contract, security, migration, or performance tests.
- No code coverage, dependency vulnerability scan, SAST, or container scan.

**Impact:** Regressions reach `master` undetected; the test suite cannot serve as a release gate; security defects are not caught.

**Fix:** Add Testcontainers (SQL Server, Redis, Kafka) or an approved embedded profile; fix stale tests; add MockMvc security tests; add `spring-boot-starter-test`/`spring-security-test` consistently; add CI with build, unit, integration, migration, and security scans; publish coverage and fail below a threshold; add Dependabot/OWASP Dependency-Check/Trivy.

**Verification:** CI is green on a clean clone; a deliberately broken auth test fails the build; coverage and scan reports are published.

**Resolution (Batch 1, partial):** The user-service isolated suite now runs 53 tests with 0 failures/errors, and `mvnw -B -DskipTests package` succeeds. Full Spring Boot suites still require SQL Server/Redis/Kafka, and no CI, coverage, or scan configuration exists. The other services' stale tests are unchanged.

**Resolution (Batch 2, partial):** Stale product and cart unit tests were repaired, and controller tests were converted to `@WebMvcTest` with security enabled. Real HMAC-signed token helpers now exercise each resource-server decoder. The five services currently report **161 tests run, 156 passed, 5 skipped, 0 failures/errors**; the five skips are `*ApplicationTests`, explicitly disabled because they require SQL Server/Redis/Kafka. All five services package successfully. No CI, coverage gate, vulnerability scan, Testcontainers integration suite, or runtime smoke test exists yet.

---

### 5.3 Medium

| ID | Finding | Evidence | Impact | Fix | Verification |
|---|---|---|---|---|---|
| M-01 | Exception messages leak internals and error shapes are inconsistent | `user-service/.../GlobalExceptionHandler.java:40-43` returns `"Internal error: " + ex.getMessage()`; cart/order/product handlers have their own formats | Stack/driver details leak; clients cannot handle errors uniformly | Return a stable error code/message, log the details server-side with a correlation ID, define one error schema | Generic exceptions return a generic message; logs contain the detail |
| M-02 | Pagination and query sizes are unbounded | `ProductController` accepts `Pageable` without a max size; order `size` is not capped; cart returns all items | Memory/CPU exhaustion; expensive sorts; cache key explosion | Cap `size` (for example 100), allowlist sort fields, add defaults | `size=100000` is rejected/clamped; invalid sort returns 400 |
| M-03 | Missing transaction boundaries and inefficient fetching | `Order` uses EAGER items; order create/save/history/event are separate steps without a single transaction; no `@Transactional` on multi-write methods | Partial writes, N+1 queries, inconsistent history | Add `@Transactional` to use cases; use `LAZY` + fetch joins/entity graphs; batch writes | Failure injection leaves no partial order/history; query counts are bounded |
| M-04 | Swagger/OpenAPI is public and lacks security schemes in four services | Product/inventory/order Swagger configs define only `Info`; all `/v3/api-docs` and `/swagger-ui` are unauthenticated | API surface disclosure; no way to authorize in the UI | Disable in production or protect with admin auth; add bearer security scheme; document auth per endpoint | Production docs are 404/401; authorized UI calls succeed |
| M-05 | Email/username uniqueness is not enforced and registration races | `UserRepository` has only `findByUsername`; no unique email; read-then-save | Duplicate accounts, inconsistent login identity | Add unique indexes; validate email; handle conflicts | Concurrent registration creates one account; duplicate email returns 409 |
| M-06 | User Kafka bootstrap is hardcoded and ignores configuration | `user-service/.../config/KafkaProducerConfig.java` uses `localhost:9092` instead of `spring.kafka.bootstrap-servers` | Deployed user service cannot reach Kafka; local-only behavior | Remove the custom config or read the property; use the Boot auto-configuration | Changing `KAFKA_HOST`/`KAFKA_PORT` changes the target |
| M-07 | No healthchecks, graceful shutdown, resource limits, or backups | `docker-compose.yml` has no `healthcheck`, `deploy.resources`, or backup job; no `server.shutdown=graceful` | Unhealthy dependencies go unnoticed; abrupt shutdown loses in-flight work; no recovery point | Add healthchecks, `depends_on: condition: service_healthy`, resource limits, graceful shutdown, backup/restore procedure | `docker compose ps` reports healthy; SIGTERM drains; restore drill passes |
| M-08 | User cache is not invalidated on password change | `UserService.java:55-66` has no `@CacheEvict`; `getUserByUsername` is `@Cacheable` | Stale hash/email/role in cached responses | Evict on password change and any user mutation; cache DTOs | Password change followed by `/me` shows fresh data |
| M-09 | No password policy or change safeguards | No constraints; `changeUserPassword` checks only the old password; no new≠old, no breach check | Weak passwords; trivial cycling | Enforce length/complexity/breach checks; reject reuse; rate-limit; document | Weak/reused passwords are rejected |
| M-10 | No security audit logging | Login, role changes, failed auth, admin writes, and config changes are not recorded with actor/IP/outcome | Forensics and detection are impossible | Add structured security events with correlation IDs, retention, and alerting; never log secrets | Audit events appear for success/failure and are queryable |
| M-11 | No explicit timeouts or connection-pool tuning | No `spring.kafka.*.timeout`, Redis timeout, or Hikari pool settings; default drivers can hang | Cascading failures and long request stalls | Set connect/read/request timeouts, bounded pools, circuit breakers with timeouts | Downstream hang fails fast with 503 |
| M-12 | API response shapes are inconsistent | Cart uses `ApiResponse<T>`; user/product/inventory return raw objects/strings; order returns raw DTOs | Client complexity and accidental contract breaks | Define a consistent envelope or consistently raw DTOs; version changes | Contract tests cover every endpoint |

### 5.4 Low

| ID | Finding | Evidence | Impact | Fix |
|---|---|---|---|---|
| L-01 | Build/log artifacts are not ignored | `.gitignore` contains only `.idea` and `.env`; `target/` and `logs/` are untracked but appear in `git status` after builds | Accidental commits of large/binary/log data | Add `target/`, `logs/`, IDE files, `.env.*` (except example), OS files |
| L-02 | README overstates implemented features | README lines 3, 12, 17, 19, 55, 59-62, 111-113, 131-133, 136-143, 166-172 | Misleads users and reviewers; obscures real gaps | Rewrite README to match reality; mark planned features explicitly |
| L-03 | Repository layout is confusing and duplicated | `nexgen-server/<service>/<service>/...`; `Inventory-Service` artifact naming; no root aggregator POM | Harder onboarding, CI, and dependency management | Flatten to one level; add a parent POM; align names |
| L-04 | No license, security policy, or contribution guide | No `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md` | Legal ambiguity; no private vulnerability reporting | Add license, security policy, contribution guide |
| L-05 | No formatting, linting, or static analysis | No Spotless/Checkstyle/PMD/Sonar/OWASP config | Style drift and missed defects | Add formatting/lint/static analysis to CI |

**Batch 1 resolution notes (partial):**
- **M-08 — Resolved:** user-profile caching was removed, so no cached password hash/profile can become stale.
- **M-09 — Partial:** registration/change-password now require 12–128 characters and reject a new password equal to the old one. There is no breach-list check, reuse history, or login rate limit yet.
- **L-01 — Resolved:** `.gitignore` now excludes `target/`, `logs/`, `.env*` (except `.env.example`), IDE, and OS files.
- **L-02 — Partial:** README now carries a NOT PRODUCTION READY banner, corrected integration/security claims, and links to the audit. The full truth pass remains Batch 9.

---

## 6. README claims versus reality

The README is a useful project overview but is not an accurate specification. Each claim below was checked against the baseline commit.

| README claim | Location | Reality at `bf41b37` | Status |
|---|---|---|---|
| "cloud-based, scalable, event-driven ... Saga Choreography" | line 3 | No cloud deployment; no saga implementation; no consumers | Inaccurate |
| "React" | line 3, 8 | No frontend code | Planned |
| "SASL_SSL Authentication" for Kafka | line 12, 111 | `PLAINTEXT`, no TLS/SASL, auto-create topics | Inaccurate |
| "Endpoints secured via Kong JWT plugin and SSL" | line 113 | Kong scripts broken; app JWT != Kong JWT; login blocked; no TLS | Inaccurate |
| "Actuator endpoints for all services" | line 119 | user-service has no Actuator dependency | Inaccurate |
| "Prometheus ... Grafana dashboards" | lines 120-121 | Only order/cart scraped; no provisioned dashboards | Partial |
| "Centralized structured logging Loki/Promtail" | line 122 | Only cart and Kong watched; logs are file-only with relative paths | Partial |
| "Resilience4j ... for Retry, Circuit Breaker, Rate Limiting" | line 129 | Only cart uses it; no service-to-service calls; fallbacks fabricate results | Partial |
| "Timeout policies configured for all service-to-service calls" | line 132 | No service-to-service calls and no timeout configuration | Inaccurate |
| "Order placed → inventory → payment → confirmation" | line 60 | No inventory/payment/notification consumers; no payment service | Inaccurate |
| "Payment integration" | line 59 | Cart returns `paymentStatus("SUCCESS") // Simulated`; no PSP | Inaccurate |
| "Notification Service" | lines 67-73 | Does not exist | Planned |
| "Full CRUD for all services with pagination" | line 170 | Cart has no pagination; inventory/cart/order behavior differs | Partial |
| "Secured microservices and API endpoints" | line 194 | Only user-service has app security; others are open | Inaccurate |
| "Docker, Docker Compose" | line 14, 158 | Compose covers infra only; no Dockerfiles | Partial |
| "SAGA rollback/compensating transactions" | lines 61, 142 | No saga, no compensation | Inaccurate |

**Action:** rewrite the README in the same change series that fixes behavior, and mark all planned/partial items explicitly. Do not claim a control exists until an automated test or operational check proves it.

---

## 7. Prioritized remediation plan (summary)

The full sequence, dependencies, and acceptance criteria are in `docs/IMPLEMENTATION_ROADMAP.md`.

| Priority | Batch | Scope | Why first |
|---|---|---|---|
| P0 | Batch 1 | User-service auth hardening: validation, role escalation, password-hash exposure, token type/TTL/claims, 401 handling, refresh/logout revocation, CORS, targeted tests | Smallest self-contained change that closes the most direct account-takeover and credential-leak paths |
| P0 | Batch 2 | Authn/authz across product, inventory, cart, order; ownership checks; deny-by-default; Kong alignment | Closes unauthenticated writes and IDOR |
| P0 | Batch 3 | Checkout/order integrity: server-side pricing, no fake payment, order-before-clear, idempotency, BigDecimal, inventory reservation | Closes fraud and data-loss paths |
| P1 | Batch 4 | Persistence: Flyway, DB init, constraints, `ddl-auto=validate`, transaction boundaries | Makes deployments reproducible and data safe |
| P1 | Batch 5 | Eventing: outbox, event envelope, DLQ/idempotency, remove fabricated fallbacks; implement or remove saga claims | Prevents silent loss/duplication |
| P1 | Batch 6 | Deployment: Dockerfiles, hardened Compose, `.env.example`, secrets, healthchecks, Kong fix, CI | Makes the system reproducible and testable |
| P2 | Batch 7 | Observability: Actuator everywhere, structured stdout logs, all metrics, correlation IDs, alerts, dashboards | Operability |
| P2 | Batch 8 | Test/quality: Testcontainers, integration/security/contract tests, coverage, SAST/dependency/container scans | Release gate |
| P3 | Batch 9 | Docs truth pass, performance/load tests, backup/restore drills, optional payment/notification/frontend with real integrations | Production readiness and scope expansion |

**Sequencing rule:** do not start Batch 2 before Batch 1 is merged and green; do not deploy any batch without the tests listed in its acceptance criteria.

---

## 8. Phase 1 gate

Phase 1 is complete when:

- [x] Every README claim has been checked against source/config/tests.
- [x] Architecture and service inventory are documented (`docs/ARCHITECTURE.md`).
- [x] Findings have file/class/method evidence, severity, impact, and a fix.
- [x] Baseline build/test results are recorded, including what could not be run.
- [x] A prioritized, dependency-aware roadmap exists (`docs/IMPLEMENTATION_ROADMAP.md`).
- [x] Security, operations, and deployment docs exist with current-state warnings.
- [ ] The user has reviewed the findings and approved the batch order. **(pending)**
- [x] Batch 1 is implemented and green in the isolated user-service suite. **Full context/integration verification is still blocked by missing infrastructure.**
- [x] Batch 2 is implemented and green in the isolated five-service suites. **Runtime/Kong/Testcontainers verification is still blocked by missing infrastructure.**

**Release recommendation: NO-GO.** The Critical findings are exploitable or block deployment. No production traffic should be sent to this system until C-01 through C-11 are closed and verified. Batch 1 closes the user-service portions of C-02/C-03/C-04, H-01/H-02, and parts of H-03/H-04. Batch 2 largely closes the service-level authentication/authorization portions of C-01/C-05, fixes the token-derived order identity in C-07, and closes the application CORS portion of H-04. C-01 is still partial because direct ports and Kong are unresolved; C-06, C-07, and C-08 through C-11 remain open.

---

## 9. Unverified items and limitations

These items were **not** verified and must not be treated as working:

1. **Docker/Compose runtime.** Docker is not installed in the audit environment. The Compose file was inspected statically; no container was started. Whether the images resolve, whether Kong migrations succeed, and whether the stack becomes healthy are unknown.
2. **Live SQL Server behavior.** No database was available. The claim that databases are missing is inferred from the absence of init scripts and the use of `ddl-auto: update`; it was not reproduced against a running SQL Server.
3. **Kafka/SASL/TLS.** No broker was available. The plaintext configuration is confirmed statically; runtime connectivity and the actual advertised listener behavior are unverified.
4. **Kong behavior.** No Kong instance was available. The service-level JWT plugin/login deadlock and the broken script filenames are confirmed by inspection; the exact Kong error responses were not observed.
5. **Exploitation.** No exploit was executed. Impact statements are based on code paths and standard attack patterns.
6. **Full test suite.** `@SpringBootTest` context classes are `@Disabled` with an explicit reason and were not executed against SQL Server/Redis/Kafka. Their results are unknown; only unit/`@WebMvcTest` suites are verified.
7. **Performance, load, and concurrency.** No benchmarks or concurrency tests were run.
8. **Azure/cloud.** No cloud resources exist or were inspected.
9. **Dependency vulnerabilities.** No SCA/CVE scan was run.
10. **Secrets in git history.** Only the current tree was searched; history was not exhaustively scanned for leaked secrets.
11. **Batch 1 application startup.** The user-service context was not started because SQL Server, Redis, and Kafka are unavailable. The 53 passing tests are unit/`MockMvc` tests only; Spring bean wiring, migrations, and live Redis/Kafka behavior remain unverified.
12. **Batch 2 application startup and cross-service auth.** Product, inventory, cart, and order contexts were not started. The 156 passing tests are unit/`@WebMvcTest` tests; token issuance by a live user-service, clock skew, live Redis revocation, CORS preflight, and every cross-service call remain unverified. The five `*ApplicationTests` context tests are explicitly skipped.

---

## Appendix A — Key evidence index

| Area | File | Lines |
|---|---|---|
| Client-controlled role | `nexgen-server/user-service/user-service/src/main/java/com/nexgen/user_service/service/UserService.java` | 23-39 |
| Password entity response | `.../user_service/controller/UserController.java` | 25-50 |
| User cache | `.../user_service/service/UserService.java` | 55-72 |
| JWT generation | `.../user_service/service/JwtService.java` | 39-67 |
| JWT filter | `.../user_service/config/JwtAuthFilter.java` | 32-74 |
| Security rules | `.../user_service/config/SecurityConfig.java` | 30-43 |
| Refresh/logout | `.../user_service/controller/AuthController.java` | 68-98 |
| Error leakage | `.../user_service/exception/GlobalExceptionHandler.java` | 40-43 |
| Cart pricing/checkout | `nexgen-server/cart-service/cart-service/src/main/java/com/nexgen/cart_service/service/CartServiceImpl.java` | 84-90, 164-188, 228-234 |
| Order trust | `nexgen-server/order-service/order-service/src/main/java/com/nexgen/order_service/service/OrderServiceImpl.java` | 43-70 |
| Order cache key | same | 85-88 |
| Inventory service | `nexgen-server/inventory-service/inventory-service/src/main/java/com/nexgen/inventory_service/service/InventoryServiceImpl.java` | full file |
| Compose | `docker-compose.yml` | 3-146 |
| Kong setup | `kong-automation/setup-all.sh` | 5-10 |
| Prometheus | `prometheus/prometheus.yml` | 4-14 |
| Promtail | `promtail-config.yaml` | 11-27 |
| Logback | `nexgen-server/*/*/src/main/resources/logback-spring.xml` | 1-30 |
| Unsafe Redis typing | `.../order_service/config/RedisConfig.java` | 39-43 |
| Unsafe Redis typing | `.../product_service/config/RedisCacheConfig.java` | 37-40 |
| Trusted packages | `.../product_service/src/main/resources/application.yaml` | 40 |

## Appendix B — Commands used

```powershell
# From each service directory:
.\mvnw.cmd -B -q -DskipTests package
.\mvnw.cmd -B "-Dtest=UserServiceTest" test
.\mvnw.cmd -B "-Dtest=ProductServiceTest,KafkaProducerServiceTest,ProductControllerTest" test
.\mvnw.cmd -B "-Dtest=InventoryServiceImplTest,KafkaInventoryProducerTest,InventoryControllerTest" test
.\mvnw.cmd -B "-Dtest=CartServiceImplTest" test
.\mvnw.cmd -B "-Dtest=OrderServiceImplTest" test
.\mvnw.cmd -B "-Dincludes=org.hibernate.validator:*,jakarta.validation:*" dependency:tree
docker version   # unavailable
```

## Appendix C — Document maintenance

- Update the baseline commit and date at the top of this file whenever the audit is refreshed.
- When a finding is fixed, add a **Resolution** line with the PR/commit, tests added, and verification evidence; do not delete the finding.
- Re-run the full test suite and the deployment smoke test before changing the release recommendation.
- Keep `docs/IMPLEMENTATION_ROADMAP.md`, `docs/SECURITY.md`, `docs/OPERATIONS_RUNBOOK.md`, and `docs/DEPLOYMENT.md` consistent with this audit.

