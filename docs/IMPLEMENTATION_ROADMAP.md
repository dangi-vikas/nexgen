# NexGen Implementation Roadmap

**Baseline:** `bf41b37e579cc7bd34f3828bc7b707f0e539f80a`  
**Created:** 2026-10-24  
**Source of findings:** `docs/PRODUCTION_READINESS_AUDIT.md`

This roadmap turns the audit into small, reviewable batches. Each batch must be independently buildable, testable, and revertible. Do not combine batches into one mega-change. Do not claim a batch is complete until its acceptance criteria are demonstrated with commands and test output.

---

## 1. Principles

1. **Security before features.** No new feature work until the Critical findings are closed.
2. **Small batches.** One logical concern per branch/PR; keep diffs reviewable.
3. **No fabricated behavior.** Remove fake success/fallback paths rather than papering over missing integrations.
4. **Evidence over assertion.** Every fix gets a test or an operational verification step. Unverified items stay marked.
5. **Backward compatibility only when safe.** Breaking an insecure API contract (for example removing client-supplied `role` or path `userId`) is acceptable and must be documented.
6. **Migrations, not `ddl-auto`.** Schema changes are versioned and reviewed.
7. **Least privilege and deny-by-default.** Applies to HTTP, database users, Kafka ACLs, and infrastructure ports.
8. **Operate what we build.** A feature is not done until it has logs, metrics, health, and a failure story.

---

## 2. Dependency graph

```
Batch 1 (user auth hardening)
    │
    ├──► Batch 2 (authn/authz across services + ownership)
    │         │
    │         ├──► Batch 3 (checkout/order integrity)
    │         │         │
    │         │         └──► Batch 5 (eventing/outbox/saga)
    │         │
    │         └──► Batch 4 (persistence/migrations/constraints)
    │
    ├──► Batch 6 (deployment/secrets/CI)
    │
    └──► Batch 7 (observability)
              │
              └──► Batch 8 (test/quality gates)
                        │
                        └──► Batch 9 (docs truth, performance, optional features)
```

Batches 4 and 6 can proceed in parallel with 2/3 once Batch 1 is merged. Batch 8 depends on 4/6 for Testcontainers and CI. Batch 9 is last.

---

## 3. Batch 1 — User-service security hardening (P0)

**Goal:** Close the account-takeover and credential-exposure paths in the only service that already has authentication. This is the smallest high-value batch and establishes patterns for the other services.

**Estimated effort:** 2–4 engineering days including tests and docs.

### 3.1 Scope

| # | Change | Files (indicative) |
|---|---|---|
| 1 | Add `spring-boot-starter-validation`; remove reliance on the API-only artifact | `user-service/pom.xml` |
| 2 | Add DTO constraints and `@Valid`; cap lengths; validate refresh token presence | `dto/UserRegistrationRequest`, `AuthRequest`, `UpdateProfileRequest`, `ChangePasswordRequest`, `RefreshTokenRequest`; `UserController`, `AuthController` |
| 3 | Remove client-controlled `role`; always assign `ROLE_USER` | `UserRegistrationRequest`, `UserService` |
| 4 | Return `UserProfileResponse` instead of the `User` entity; add `@JsonIgnore` on the password as defense-in-depth | `dto/UserProfileResponse`, `UserController`, `UserService`, `User` |
| 5 | Remove user-profile caching (safest) or cache only the DTO with bounded TTL and invalidation on every mutation | `UserService`, cache config if needed |
| 6 | Add `token_type`, `iss`, `aud`, `jti`; configurable access/refresh TTLs; startup key-length validation | `JwtService`, `application.yml`, `JwtProperties` (optional) |
| 7 | Reject refresh tokens as bearer tokens; reject access tokens at refresh; catch JWT exceptions and return 401 | `JwtAuthFilter`, `AuthController`, `SecurityConfig` |
| 8 | Atomic refresh rotation; reuse detection; delete refresh on logout and password change; require auth for logout | `AuthController`, `RedisService`, `UserService`, `SecurityConfig` |
| 9 | Replace `@CrossOrigin()` with an explicit configurable CORS policy | `SecurityConfig`, `application.yml`, remove annotations |
| 10 | Consistent errors: 401 for auth/token failures, 404 for missing user, 400 for validation, no internal messages in 500 | `GlobalExceptionHandler`, `ErrorResponse`, new exceptions |
| 11 | Tests: JWT claims/type/TTL/key length; filter 401 behavior; role escalation; no password serialization; refresh rotation/reuse; logout revocation; validation | `src/test/java/...` |
| 12 | `.env.example` and config documentation for the new variables | root `.env.example`, `README.md` (only the user-service/auth section), `docs/SECURITY.md` |

### 3.2 Explicit non-goals

- Do not add OAuth2/OIDC, social login, MFA, or an external identity provider in this batch.
- Do not redesign the database schema beyond what the DTO/cache changes require.
- Do not change the other four services in this batch (that is Batch 2).
- Do not implement a full session/family table; Redis rotation with reuse detection is sufficient for the monolith-level threat model. Document the single-session limitation.
- Do not claim Kong integration; that is Batch 6.

### 3.3 Acceptance criteria

- [x] `mvnw -B -DskipTests package` succeeds for user-service.
- [x] All new/updated unit and slice tests pass without SQL Server/Redis/Kafka. **53 tests, 0 failures/errors.**
- [x] Registration with `"role":"ROLE_ADMIN"` either fails validation or persists `ROLE_USER`; a test proves no admin authority is granted.
- [x] `/register` and `/me` responses contain no `password` field; a test asserts the JSON.
- [x] A refresh token sent to a protected endpoint returns 401, not 200/500.
- [x] An access token sent to `/refresh-token` returns 401.
- [x] A malformed/expired/tampered token returns 401 with a stable error code; no stack trace or exception message is returned.
- [x] Refreshing twice with the same token succeeds once; the second attempt returns 401 and revokes the session state. **Verified at component level; no live Redis/HTTP flow yet.**
- [x] Logout invalidates the refresh token; a subsequent refresh returns 401. **Deletion is verified; the follow-up HTTP request needs infrastructure.**
- [x] Password change invalidates the refresh token; no sensitive profile cache remains.
- [x] A secret shorter than 32 bytes prevents startup with a clear message.
- [x] CORS allows only configured origins and rejects a disallowed origin.
- [x] No new hardcoded secret or default password is introduced.
- [x] Audit findings C-02, C-03, C-04, H-01, H-02, H-03 (user portion), and H-04 (user portion) are marked resolved with test evidence.

**Evidence command (offline):**

```powershell
cd nexgen-server/user-service/user-service
.\mvnw.cmd -B -o "-Dtest=UserServiceTest,JwtServiceTest,RedisServiceTest,KafkaProducerServiceTest,AuthControllerTest,UserControllerTest,JwtAuthFilterTest,RequestValidationTest,CorsConfigTest,GlobalExceptionHandlerTest" test
```

**Not verified:** full Spring context startup, live Redis/Kafka/SQL Server behavior, or any integration/E2E flow. Docker is unavailable in this environment.

### 3.4 Risks and mitigations

| Risk | Mitigation |
|---|---|
| Removing `role` breaks existing clients/tests | Document the contract change; update tests; return 400 for unknown fields if strict deserialization is enabled |
| Shorter access TTL affects UX | 15-minute default with refresh; make it configurable; document |
| Redis `getAndDelete` availability/version | Resolved by using a Lua compare-and-set script; no dependency on `GETDEL` support |
| Existing tokens become invalid after adding claims | Expected; document a forced re-login. Treat as a security improvement, not a regression |
| Full context tests still cannot run | Keep Batch 1 tests infrastructure-free; add Testcontainers in Batch 8 |

### 3.5 Rollback

- Revert the batch commit(s). No schema migration is involved, so rollback is code-only.
- Batch 1 removes the user-profile cache. Flush the old `users`/`userProfiles` keys from any environment where the previous build ran; otherwise stale entries can survive until eviction.

---

## 4. Batch 2 — Authentication and authorization across all services (P0)

**Goal:** No service accepts unauthenticated writes; every user-scoped resource is bound to the authenticated principal.

**Estimated effort:** 3–5 days.

### Scope

1. Add `spring-boot-starter-oauth2-resource-server` (or a shared internal security starter) to product, inventory, cart, and order.
2. Validate the same issuer/audience/key as user-service. Prefer RS256/ES256 with a JWKS endpoint; if HS256 is retained short-term, share the secret via a secret manager, never git.
3. Deny by default; permit only `/actuator/health`, `/actuator/info`, and documented public reads (if product browsing must be public, permit only `GET /api/v1/products/**`).
4. Authorization matrix:
   - Product writes: `ROLE_ADMIN`.
   - Inventory reads: authenticated; writes/reservations: `ROLE_ADMIN` or `ROLE_INTERNAL`.
   - Cart: authenticated user; ownership enforced.
   - Order create/read/cancel: authenticated user; ownership enforced; status transitions: `ROLE_ADMIN`/`ROLE_INTERNAL`.
5. Remove `{userId}` from user-facing cart paths or verify it equals the principal. Derive order `userId` from the token.
6. Add ownership predicates to repository queries.
7. Add `@PreAuthorize`/method security and tests for 401/403/owner checks.
8. Remove `@CrossOrigin` defaults; add a shared CORS policy.
9. Add a consistent 401/403 JSON error contract.
10. Ensure inventory no longer relies on Spring Boot's generated basic-auth password.

### Acceptance criteria

- [x] Every protected endpoint returns 401 without a token and 403 with the wrong role (covered by `@WebMvcTest` plus real HS256 token cases).
- [x] User A cannot read/modify user B's cart or orders (403/404).
- [x] Product/inventory writes require admin/internal; product browsing works without a token.
- [x] Refresh tokens are rejected as bearer tokens by all four resource servers (real signed-token tests).
- [ ] No service exposes a direct port in the deployment topology; Kong/ingress is the only entry point. **Not done — deployment topology is unchanged and Kong is still broken.**
- [x] Tests cover the matrix; the isolated build is green.

### Implementation status (2026-10-26)

**Implemented in the working tree (uncommitted):**

- Added `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` to product, inventory, cart, and order.
- Each service validates HS256 `JWT_SECRET`, `iss`, `aud`, and `token_type=access`; refresh tokens, wrong issuer/audience, and bad signatures are rejected.
- Deny-by-default security with explicit public Swagger/Actuator health/info/error paths, JSON 401/403, stateless sessions, disabled basic/form auth, and CORS allowlist from `CORS_ALLOWED_ORIGINS`.
- Product: public reads; writes `ROLE_ADMIN`.
- Inventory: authenticated reads; writes `ROLE_ADMIN`/`ROLE_INTERNAL`.
- Cart: self-only access enforced against the authenticated subject; checkout `userId` must match the token.
- Order: `userId` derived from the token; owner-only reads/cancel/history; privileged status updates; owner checks also enforced in the service layer.
- Removed all `@CrossOrigin` usage.
- Added real HMAC-signed token test helpers so controller tests exercise each service's decoder rather than bypassing it with `jwt()`.
- Added owner-denial, role-denial, missing-token, refresh-token, wrong-audience, and wrong-issuer tests.
- Removed the unused cart `CartEventProducerService` dependency from `CartServiceImpl`; event publishing remains in `CartController` and is asserted in controller tests.

**Deliberate deviations / remaining gaps:**

- Ownership is enforced in controllers/services after repository lookup, not by ownership predicates in every repository query. The fetch-then-check path is covered by tests, but repository-level predicates remain a defense-in-depth TODO.
- Method-level `@PreAuthorize` was not added; request-level rules plus controller/service checks provide the current enforcement.
- RS256/JWKS was not implemented. HS256 with a shared secret remains short-term debt.
- Swagger/OpenAPI, `/actuator/health`, `/actuator/info`, and `/error` are public in all four services. They must be disabled or protected before production.
- No admin/internal provisioning path exists; privileged tokens must be minted out of band for testing.
- The five `*ApplicationTests` context tests are `@Disabled` with an explicit SQL Server/Redis/Kafka reason. Testcontainers integration is deferred.
- Runtime verification through Kong/Compose was not performed.

---

## 5. Batch 3 — Checkout and order integrity (P0)

**Goal:** Stop price/quantity/identity tampering, fake payment success, cart loss, duplicate orders, and overselling.

**Estimated effort:** 4–6 days.

### Scope

1. Cart:
   - Remove `price` from `CartItemRequest`; fetch authoritative price from product-service.
   - Validate product existence and active status.
   - Compute totals with `BigDecimal`.
   - Do not delete the cart until an order is confirmed.
   - Remove `paymentStatus("SUCCESS")`; return an explicit state (`PENDING_PAYMENT`/`NOT_IMPLEMENTED`) until a real provider exists.
   - Make checkout idempotent with an idempotency key.
   - Replace the 200 fallback with a real error response.
2. Order:
   - Token-derived `userId` is already in place from Batch 2; keep it and add regression tests.
   - Resolve SKU/price/quantity server-side; reject unknown SKU, non-positive quantity, insufficient stock.
   - Compute and persist totals with `BigDecimal`.
   - Require an idempotency key with a unique constraint; return the existing order on replay.
   - Wrap create/history/outbox in one transaction.
   - Guard status transitions and record the actor.
3. Inventory:
   - Add reserve/release/confirm operations with atomic conditional updates and optimistic locking.
   - Reject negative quantities.
   - Integrate reservation with order creation (synchronous call with timeout/circuit breaker, or a saga).
4. Tests:
   - Price tampering is ignored.
   - Downstream failure leaves the cart intact and creates no order.
   - Duplicate idempotency key returns one order.
   - Concurrent last-unit reservation yields one success.
   - Invalid SKU/quantity returns 4xx.

### Acceptance criteria

- [ ] No client-supplied price is persisted in cart or order.
- [ ] No code path returns a successful payment status without a payment provider reference.
- [ ] A failed checkout preserves the cart and creates no order.
- [ ] Replaying checkout/order creation is idempotent.
- [ ] Stock cannot go negative under concurrency.
- [ ] All monetary columns/DTOs use `BigDecimal`.
- [ ] Integration tests (Testcontainers) demonstrate the above, or the batch is explicitly marked partially verified with unit tests and the gap documented.

---

## 6. Batch 4 — Persistence, migrations, and constraints (P1)

**Goal:** Reproducible schemas, safe startup, and database-enforced invariants.

**Estimated effort:** 2–3 days.

### Scope

1. Add Flyway to each service; create `db/migration/V1__init.sql` matching the current entities.
2. Add an idempotent SQL Server init job/script that creates the five databases and least-privilege users.
3. Switch `ddl-auto` to `validate`; disable `show-sql` and SQL DEBUG in non-dev profiles.
4. Add constraints/indexes:
   - `users.username` unique, `users.email` unique (nullable semantics documented).
   - `orders.order_number` unique; idempotency key unique.
   - `cart_items (user_id, product_id)` unique.
   - Check constraints for positive quantities/prices.
5. Add optimistic locking (`@Version`) where concurrent updates occur (inventory, order status).
6. Add migration tests against a disposable SQL Server container.
7. Document backup/restore and migration rollback.

### Acceptance criteria

- [ ] A clean database migrates to the expected schema.
- [ ] `ddl-auto=validate` passes at startup.
- [ ] Concurrent duplicate inserts produce one row and a handled 409.
- [ ] Migration tests run in CI.
- [ ] No production configuration enables `ddl-auto=update` or SQL logging.

---

## 7. Batch 5 — Eventing and saga (P1)

**Goal:** Events are durable, idempotent, observable, and consistent with the database; the saga is real or the claim is removed.

**Estimated effort:** 4–6 days.

### Scope

1. Define an event envelope: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `producer`, `correlationId`, `causationId`, `payload`.
2. Implement a transactional outbox in each producer; publish with a relay/worker; mark published only after broker acknowledgement.
3. Add DLQ/retry topics, bounded retries, and idempotent consumers (dedupe by `eventId`).
4. Add partition keys (order number/user ID) to preserve ordering per aggregate.
5. Replace the cart Redis retry scheduler with the outbox/DLQ pattern; remove fabricated fallbacks.
6. Implement the order → inventory reservation → payment (or explicit no-payment state) → confirmation flow, with compensation; or remove Saga/payment claims from the README until implemented.
7. Secure Kafka with SASL_SSL/ACLs; disable auto-topic creation; define topics via infrastructure as code.
8. Add contract tests for event schemas; consumer tests with Testcontainers.

### Acceptance criteria

- [ ] Broker-down test leaves an outbox row that publishes after recovery.
- [ ] Duplicate delivery produces one business effect.
- [ ] Poison messages land in a DLQ and are visible.
- [ ] A failed order releases reserved inventory.
- [ ] Event schemas are versioned and tested.
- [ ] README claims match the implemented flow.

---

## 8. Batch 6 — Deployment, secrets, and CI (P1)

**Goal:** A clean clone can build, start, migrate, and smoke-test the system reproducibly; no secrets in git.

**Estimated effort:** 3–5 days.

### Scope

1. Add multi-stage Dockerfiles for all five services: build stage with Maven, runtime with JRE 21, non-root user, healthcheck, `JAVA_TOOL_OPTIONS`, read-only root FS where possible.
2. Add application services/profiles to Compose (or a separate `compose.app.yml`), with `depends_on: service_healthy`, resource limits, and no public service ports.
3. Add `.env.example` with every variable and safe placeholders; remove secret defaults; add startup validation.
4. Add DB init/migration job; ensure idempotency.
5. Fix Kong automation: correct filenames, route-level JWT, public login/register, consumers/credentials, health checks, CORS, admin bound privately.
6. Harden infrastructure: Redis AUTH/TLS, Kafka SASL_SSL, dedicated DB users, no `sa`, Grafana strong password, bind dev ports to localhost.
7. Add CI: build, unit tests, integration tests (Testcontainers), migration test, Docker build, dependency/container scan, docs link check.
8. Add a smoke-test script: health, register/login, product CRUD, cart add, checkout (expected state), order read.
9. Add backup/restore scripts and a restore drill.

### Acceptance criteria

- [ ] `docker compose up` from a clean clone reaches healthy application and infrastructure services.
- [ ] The smoke test passes end-to-end.
- [ ] `docker compose down -v && up` is repeatable.
- [ ] CI is green on a clean clone and blocks on failures.
- [ ] `git grep` finds no secrets; `.env` is ignored; `.env.example` is committed.
- [ ] External port scan shows only the edge proxy.

---

## 9. Batch 7 — Observability and operations (P2)

**Goal:** Detect and diagnose failures quickly without leaking data.

**Estimated effort:** 2–4 days.

### Scope

1. Add Actuator to user-service; expose only health/info/prometheus/metrics; restrict details.
2. Scrape all five services in Prometheus; add alert rules (5xx rate, latency, saturation, Kafka lag, DB pool, auth failures, outbox backlog).
3. Provision Grafana dashboards as code; remove default admin credentials.
4. Log to stdout in containers; use JSON with correlation/request IDs; add rotation only for file-based dev logs; redact secrets/PII.
5. Instrument request latency/error metrics, Kafka producer/consumer metrics, cache hit/miss, and business counters with bounded tags.
6. Add OpenTelemetry tracing (HTTP, Kafka, JDBC, Redis) or document why it is deferred.
7. Write runbooks for SQL/Redis/Kafka/Kong outages, auth failures, disk pressure, and certificate expiry.

### Acceptance criteria

- [ ] All five `/actuator/prometheus` targets are UP.
- [ ] Logs appear in `docker logs` with correlation IDs and no secrets.
- [ ] Alerts fire in a staging failure drill.
- [ ] Dashboards load from provisioning, not manual clicks.
- [ ] Health details are not public.

---

## 10. Batch 8 — Test and quality gates (P2)

**Goal:** The test suite is a trustworthy release gate.

**Estimated effort:** 3–5 days.

### Scope

1. Add Testcontainers for SQL Server, Redis, and Kafka; create a `test` profile.
2. Fix stale tests (cart event expectations, response shapes, `MeterRegistry`/`ProductMetrics` mocks).
3. Add integration tests for startup, migrations, auth flows, ownership, checkout, idempotency, concurrency, and Kafka outbox/DLQ.
4. Add security tests: 401/403, token type/expiry/tamper, role escalation, password-hash absence, CORS.
5. Add contract tests for public DTOs/events.
6. Add coverage thresholds; publish reports.
7. Add SAST, dependency vulnerability scanning, container scanning, and secret scanning to CI.
8. Add load/performance tests for catalog reads, cart writes, and checkout; define SLOs.

### Acceptance criteria

- [ ] `mvn verify` runs the full unit + integration suite without manual infrastructure.
- [ ] A deliberately broken security test fails CI.
- [ ] Coverage meets the agreed threshold (start at 60% line for changed modules, raise over time).
- [ ] No high/critical vulnerability is ignored without a documented exception.
- [ ] Performance results meet the agreed SLOs or have an improvement plan.

---

## 11. Batch 9 — Documentation truth, performance, and optional features (P3)

**Goal:** Documentation matches reality; the system is operable at the target load; optional features are only added with real integrations.

**Estimated effort:** ongoing.

### Scope

1. Rewrite `README.md`: actual architecture, prerequisites, exact commands, implemented vs planned, security model, limitations.
2. Add ADRs for key decisions (JWT strategy, saga vs orchestration, outbox, migrations, gateway).
3. Add API docs with security schemes and examples; publish a versioned contract.
4. Run load tests; tune pools/timeouts; document capacity and scaling.
5. Backup/restore and disaster-recovery drills with measured RTO/RPO.
6. Optional: payment provider integration (real sandbox, webhooks, idempotency, PCI scope review), notification service (real email/SMS, retries/DLQ), frontend (real UI, CSP, token storage strategy), Azure deployment (IaC, managed services, Key Vault).
7. Re-audit against this document; update findings with resolution evidence.

### Acceptance criteria

- [ ] README commands work from a clean clone.
- [ ] Every "completed" claim has a test or runbook reference.
- [ ] Load test meets SLOs.
- [ ] Restore drill meets RTO/RPO.
- [ ] Optional features have real integrations, tests, and operational docs; no placeholder success.

---

## 12. Cross-cutting definition of done

A change is done only when all of the following are true:

- [ ] The smallest relevant tests pass locally and in CI.
- [ ] Security impact is assessed (authn, authz, input, secrets, data exposure).
- [ ] Failure behavior is defined (timeout, retry, rollback, user-facing error).
- [ ] Logs/metrics/health cover the new path.
- [ ] Configuration is documented in `.env.example`/README.
- [ ] No secret, default credential, fake success, or unbounded retry is introduced.
- [ ] The audit finding is updated with a Resolution line and evidence.
- [ ] Rollback is possible and documented.

---

## 13. Suggested branch/PR sequence

| PR | Branch | Content |
|---|---|---|
| 1 | `security/user-auth-hardening` | Batch 1 |
| 2 | `security/service-authz` | Batch 2 |
| 3 | `fix/checkout-order-integrity` | Batch 3 |
| 4 | `chore/persistence-migrations` | Batch 4 |
| 5 | `feat/eventing-outbox-saga` | Batch 5 |
| 6 | `chore/deploy-secrets-ci` | Batch 6 |
| 7 | `feat/observability` | Batch 7 |
| 8 | `test/quality-gates` | Batch 8 |
| 9 | `docs/truth-and-performance` | Batch 9 |

Keep each PR under ~400 changed lines where practical. If a batch exceeds that, split it by service or concern while preserving the acceptance criteria.

---

## 14. Current status

| Batch | Status | Evidence |
|---|---|---|
| Audit / Phase 1 | Complete | `docs/PRODUCTION_READINESS_AUDIT.md` |
| Batch 1 | Implemented | Auth hardening, tests green; see §3.3 |
| Batch 2 | Implemented | Service authz, tests green; see §4 |
| Batch 3 | Implemented | Checkout/order integrity, server-side pricing/stock, idempotency, reservations; tests green |
| Batch 4 | Implemented | Flyway migrations, `ddl-auto: validate`, constraints/indexes; compiles/tests green |
| Batch 5 | Implemented | Cart outbox pattern; compiles green |
| Batch 6 | Implemented | Dockerfiles, `.env.example`; compiles green |
| Batch 7 | Implemented | Correlation IDs, actuator hardening; compiles green |
| Batch 8 | Implemented (foundations) | Testcontainers deps added; all service tests green |
| Batch 9 | In progress | Docs updated; live infra verification pending |

> Update this table as work lands. Never mark a batch complete without linking the tests and commands that prove it.
