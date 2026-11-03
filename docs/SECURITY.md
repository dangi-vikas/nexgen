# NexGen Security

**Status:** Baseline security documentation plus Batch 1 (user-service) and Batch 2 (service authentication/authorization) working-tree deltas. It describes the current posture, the required target posture, and the standards every change must meet. It does not claim that the target controls are implemented.

**Last reviewed:** 2026-10-26 at commit `bf41b37`, with uncommitted Batch 1 and Batch 2 changes  
**Related:** `docs/PRODUCTION_READINESS_AUDIT.md`, `docs/IMPLEMENTATION_ROADMAP.md`

---

## 1. Security posture summary

| Area | Current state | Target state |
|---|---|---|
| Service authentication | All five services validate the user-service JWT; product/inventory/cart/order are resource servers with deny-by-default rules (Batch 2) | Every service is a resource server; RS256/JWKS; deny by default |
| Authorization | Product/inventory role-gated; cart self-only; order owner/privileged checks (Batch 2) | Role + ownership on every operation; repository predicates; audited admin overrides |
| Registration | Server-assigned `ROLE_USER` in Batch 1; no admin provisioning path | Role is server-assigned; admin provisioning is separate and audited |
| Password storage | BCrypt; hashes no longer returned; 12-character minimum | BCrypt/Argon2 with policy; hashes never leave the DB |
| Token model | HS256 with `token_type`, `iss`, `aud`, `jti`; 15m access / 7d refresh (Batch 1) | Asymmetric signing, key rotation, JWKS |
| Refresh/logout | Hashed refresh state, atomic rotation, replay revocation; logout/password change revoke (Batch 1) | Per-device/family revocation and global token version |
| Input validation | Enforced in user-service (Batch 1); product/cart partial; order items and inventory still weak | Bean Validation everywhere; size/range/format limits |
| Transport | HTTP; Kafka PLAINTEXT; SQL `trustServerCertificate=true` | TLS everywhere; SASL_SSL; validated DB certificates |
| Secrets | JWT secret validated at startup; `.env.example` added; DB default remains | No defaults; secret manager |
| CORS | Explicit allowlist in all five services; no `@CrossOrigin` remains; Kong wildcard remains | Explicit allowlist at every edge |
| Rate limiting | Kong only, bypassable via direct ports | Edge + auth-service limits; no direct access |
| Audit logging | None | Structured security events with retention and alerts |
| Dependencies/scans | None | SCA/SAST/container/secret scanning in CI |

**Do not expose this system to untrusted networks until the Critical findings are closed.**

### 1.1 Batch 1 implemented controls (user-service only)

- Public registration cannot set `role`; the service persists `ROLE_USER`. Legacy `role` input is explicitly ignored.
- `/register` and `/me` return `UserProfileResponse`; `User.password` is `@JsonIgnore`; user-profile caching was removed.
- Access and refresh tokens are typed and carry issuer, audience, `jti`, and roles. TTLs are configurable; a weak secret or nonsensical TTL fails startup.
- Refresh tokens are stored as SHA-256 hashes, rotated atomically by a Lua compare-and-set script, and a replayed token revokes the current session. An unknown token is rejected without deleting valid state. Logout requires authentication and revokes both token classes; password change revokes refresh state.
- The JWT filter accepts only access tokens and returns JSON 401 for malformed/expired/tampered/revoked tokens and 503 when Redis is unavailable.
- Bean Validation and field-level 400 responses cover user-service request DTOs.
- CORS uses a configured origin allowlist; controller-level `@CrossOrigin` was removed.
- 53 isolated unit/`MockMvc` tests pass. Full context and integration behavior remain unverified.

**Still open:** rate limiting/lockout, audit logging, secret-manager integration, TLS/SASL, CI security gates, RS256/JWKS, admin provisioning, and Docker/Kong runtime verification.

### 1.2 Batch 2 implemented controls (product, inventory, cart, order)

- All four services are OAuth2 resource servers using the shared HS256 `JWT_SECRET`; they validate signature, issuer, audience, expiry, and `token_type=access`. Refresh tokens are rejected.
- Security is deny-by-default. Public exceptions are product reads, Swagger/OpenAPI, `/actuator/health`, `/actuator/info`, and `/error`.
- Basic auth, form login, and generated user-details auto-configuration are disabled; sessions are stateless.
- Authentication failures return JSON 401 and authorization failures return JSON 403.
- CORS uses a per-service `CORS_ALLOWED_ORIGINS` allowlist with `allowCredentials=false`; no `@CrossOrigin` remains.
- Product: reads public; writes `ROLE_ADMIN`.
- Inventory: reads authenticated; writes `ROLE_ADMIN` or `ROLE_INTERNAL`.
- Cart: all operations self-only; path/body `userId` must equal the authenticated subject.
- Order: creation derives `userId` from the token; reads/cancel/history are owner-only unless admin/internal; status updates require admin/internal.
- Real HMAC-signed token tests cover missing tokens, refresh tokens, wrong issuer, wrong audience, wrong role, and cross-user access. The isolated five-service suite reports 161 tests run, 156 passed, 5 skipped, 0 failures/errors.
- The five skipped tests are `*ApplicationTests` context tests, disabled because SQL Server, Redis, and Kafka are unavailable in this environment.

**Still open after Batch 2:** repository-level ownership predicates, audit logging for privileged overrides, admin/internal provisioning, Swagger protection/disablement, public Actuator health/info, direct service ports, Kong policy, RS256/JWKS, rate limiting, and integration/runtime tests.

---

## 2. Threat model

### 2.1 Assets

- User credentials and password hashes.
- Access/refresh tokens and session state.
- Product prices and catalog integrity.
- Inventory levels and reservations.
- Orders, payment status, and order history.
- Infrastructure credentials (SQL SA, Redis, Kafka, Kong admin, Grafana).
- Customer PII (username, email, IP in login events).
- Event streams and caches.

### 2.2 Actors

| Actor | Capability |
|---|---|
| Anonymous internet user | Can reach exposed ports; can register/login; can read the public product catalog; cannot write without a token |
| Authenticated customer | Has a valid access/refresh token; can attempt IDOR and price tampering |
| Malicious insider/operator | Can access infrastructure, logs, and caches |
| Compromised dependency | Can execute in-process or exfiltrate data |
| Network attacker | Can intercept plaintext HTTP/Kafka/SQL traffic |

### 2.3 Trust boundaries

1. Internet → edge gateway.
2. Edge → services.
3. Service → service.
4. Service → SQL Server / Redis / Kafka.
5. Application → observability stack.
6. CI/CD → runtime secrets.

Every boundary must authenticate, authorize, encrypt, and log as appropriate. Today, boundary 2 has service-level JWT validation but no working gateway; boundaries 3–6 remain weak or absent (plaintext transport, no audit logging, no CI security gates).

### 2.4 Primary threats

- Account takeover via role mass assignment, token confusion, refresh replay, or brute force.
- Credential theft via password-hash exposure or plaintext transport.
- Fraud via client-controlled prices/quantities and fake payment success.
- Horizontal privilege escalation via path/body `userId` (cross-user cart/order paths are now checked; repository-level predicates and privileged audit logging remain open).
- Data loss/corruption via `ddl-auto=update`, missing constraints, and non-atomic checkout.
- Supply-chain compromise via unsafe deserialization or vulnerable dependencies.
- Infrastructure compromise via default credentials and exposed admin ports.

---

## 3. Authentication standard

### 3.1 Token requirements

Every JWT must contain and validate:

| Claim | Requirement |
|---|---|
| `sub` | User identifier (username or stable ID) |
| `iss` | Expected issuer, validated |
| `aud` | Expected audience, validated |
| `iat` | Issued-at |
| `exp` | Expiry; access ≤ 15 minutes, refresh ≤ 7 days (configurable) |
| `jti` | Unique token ID for revocation/reuse detection |
| `token_type` | `access` or `refresh`; enforced at every consumer |
| `sid` | Session/family ID for revocation |
| `roles`/`scope` | Least-privilege authorities; optional `ver` for global revocation |

- Sign with RS256/ES256 and publish a JWKS endpoint, or HS256 with a ≥32-byte secret from a secret manager. Never commit the secret.
- Validate signature, algorithm allowlist, issuer, audience, expiry, and type. Reject `alg: none` and algorithm confusion.
- Do not put PII or secrets in claims; JWTs are readable.
- Access tokens are never accepted at the refresh endpoint; refresh tokens are never accepted as bearer credentials.
- Refresh tokens are stored hashed (or as a server-side session reference), rotated atomically, and revoked on reuse/logout/password change.
- Clock skew allowance ≤ 60 seconds.

### 3.2 Password standard

- Minimum 12 characters (or a passphrase), maximum 128; allow all characters; do not truncate silently.
- Check against a breached-password list (for example k-anonymity API) if available; reject common passwords.
- BCrypt strength ≥ 12 or Argon2id with tuned parameters.
- No password in logs, responses, events, or cache.
- Reject new password equal to the old one; revoke sessions on change.
- Rate-limit and lock out after repeated failures with a safe unlock path.
- MFA is recommended for admin accounts; not implemented today.

### 3.3 Session standard

- Short-lived access tokens; refresh rotation with reuse detection.
- One session per device/family; users can revoke sessions.
- Logout revokes the access token (blacklist until expiry) and the refresh family.
- Password change/reset revokes all sessions.
- Redis stores only the minimum state, with TTLs; hash tokens at rest.
- Define behavior when Redis is unavailable: fail closed with 503, not 500 or bypass.

---

## 4. Authorization standard

### 4.1 Roles

| Role | Permissions |
|---|---|
| `ROLE_USER` | Own profile, own cart, own orders, catalog read |
| `ROLE_ADMIN` | Catalog and inventory writes, all orders, user administration |
| `ROLE_INTERNAL` | Service-to-service operations (stock reservation, status updates) with mTLS or signed requests |

Roles are server-assigned. No public endpoint accepts a role. Admin provisioning is a separate, audited process.

### 4.2 Enforcement matrix

| Operation | Anonymous | User | Admin | Internal |
|---|---:|---:|---:|---:|
| Register/login/refresh | ✅ | ✅ | ✅ | — |
| Read catalog | ✅ (if product browsing is public) | ✅ | ✅ | ✅ |
| Write catalog | ❌ | ❌ | ✅ | — |
| Read own cart/orders | ❌ | ✅ | ✅ | — |
| Read another user's cart/orders | ❌ | ❌ | ✅ | ✅ (scoped) |
| Checkout own cart | ❌ | ✅ | ✅ | — |
| Update order status | ❌ | ❌ | ✅ | ✅ |
| Reserve/release stock | ❌ | ❌ | ✅ | ✅ |
| Actuator health/info | ✅ (minimal) | ✅ | ✅ | ✅ |
| Actuator metrics/prometheus | ❌ | ❌ | ✅/internal | ✅ |

- Deny by default: only explicitly listed routes are public.
- Ownership is enforced in the query (`WHERE owner = :principal`), not by fetch-then-compare.
- Use `@PreAuthorize`/method security and test every row.
- Never trust `userId` from a path or body for user-scoped operations.

### 4.3 Gateway

- The edge terminates TLS, rate-limits, and validates the application issuer/audience.
- Do not run two unrelated JWT systems (Kong credentials vs application tokens).
- Do not apply auth to login/register; do apply it to everything else.
- Kong admin API is private, authenticated, and never exposed to the internet.
- Direct service ports are not reachable from untrusted networks.

---

## 5. Data protection

- **In transit:** TLS 1.2+ for HTTP, SQL, Redis, Kafka. No `trustServerCertificate=true` outside local dev. SASL_SSL for Kafka.
- **At rest:** database encryption (TDE or disk encryption), encrypted backups, encrypted Redis persistence where used.
- **Minimization:** collect only necessary PII; do not log passwords, tokens, full PANs, or secrets.
- **Retention:** define retention for logs, events, and PII; support deletion requests.
- **Caching:** never cache password hashes or raw tokens; use DTOs; set TTLs.
- **Deserialization:** no default typing; strict allowlists for polymorphic types; `spring.json.trusted.packages` limited to application packages.
- **Error responses:** stable codes/messages; no stack traces, SQL, class names, or internal hostnames.
- **Backups:** encrypted, access-controlled, restore-tested.

---

## 6. Secrets management

1. No secret has a default in code or config. Missing secrets fail startup with a clear message.
2. Local development uses `.env` (git-ignored) created from `.env.example` with placeholder values.
3. Deployed environments use a secret manager (Azure Key Vault, AWS Secrets Manager, Vault, Kubernetes Secrets with encryption).
4. CI uses short-lived credentials/OIDC, not long-lived keys in repository settings.
5. Rotate on exposure and on a schedule; document owners.
6. Never print secrets in logs, error messages, or CI output.
7. Use distinct credentials per service/environment; no shared `sa`.

Required variables (current and target) are documented in `docs/DEPLOYMENT.md`.

---

## 7. Application security controls

### 7.1 Input and output

- Bean Validation on every request DTO and path/query parameter.
- Allowlist fields; reject unknown fields where feasible.
- Cap string lengths, page sizes, item counts, and request body size.
- Parameterized queries only (JPA/repositories); no string-concatenated SQL.
- Encode output; return DTOs, never entities with sensitive fields.
- Normalize/validate SKUs, usernames, and emails; avoid case/Unicode confusion.

### 7.2 Business logic

- Server-side pricing and totals (`BigDecimal`).
- Positive quantities; bounded baskets.
- Idempotency keys for checkout/order creation.
- Explicit state machine for order status; no arbitrary transitions.
- Stock reservation with expiry and compensation.
- No fabricated success; failures return errors and preserve state.

### 7.3 HTTP

- Security headers: HSTS, `X-Content-Type-Options`, `X-Frame-Options`/CSP as applicable, `Referrer-Policy`.
- Strict CORS allowlist; no `*` with credentials.
- CSRF disabled only for stateless bearer-token APIs; if cookies are introduced, enable CSRF protection.
- Rate limits at edge and auth service.
- Request timeouts and body limits.
- Swagger disabled or admin-protected in production.

### 7.4 Logging and monitoring

- Log authentication successes/failures, authorization denials, admin actions, and configuration changes with actor, IP, correlation ID, and outcome.
- Never log tokens, passwords, secrets, or full PII.
- Alert on spikes in 401/403/5xx, repeated login failures, unusual checkout volume, and DLQ growth.
- Retain security logs according to policy and protect them from tampering.

---

## 8. Secure development and supply chain

- Pin dependency versions; use the Maven wrapper; review transitive dependencies.
- Enable Dependabot/Renovate and OWASP Dependency-Check or equivalent.
- Run SAST (CodeQL/Semgrep) and secret scanning on every PR.
- Build container images from minimal, pinned bases; run as non-root; scan with Trivy/Grype.
- Generate an SBOM.
- Sign images and verify signatures before deploy (target).
- Require code review for security-sensitive paths (auth, crypto, payments, infra).
- Never commit `.env`, keys, certificates, or credentials. If leaked, rotate immediately and purge history.

---

## 9. Verification requirements

Every security fix must include automated tests where feasible:

| Control | Test |
|---|---|
| Authn | No token → 401; expired/tampered/wrong-type → 401 |
| Authz | Wrong role → 403; owner mismatch → 403/404 |
| Role assignment | Registration with `ROLE_ADMIN` cannot obtain admin authority |
| Data exposure | `/register`, `/me`, caches contain no password/hash |
| Refresh | Rotation one-time; reuse detected; logout/password change revokes |
| Validation | Invalid payloads → 400 with field errors |
| CORS | Allowed origin succeeds; disallowed fails |
| Secrets | Startup fails on missing/weak secret; `git grep` clean |
| Rate limit | Repeated failures throttled |
| Deserialization | Non-allowlisted type rejected |
| Dependencies | No high/critical CVEs without documented exception |

Tests that require infrastructure should use Testcontainers and run in CI (Batch 8).

---

## 10. Incident response (minimal)

1. **Detect:** alert or report; preserve logs and evidence.
2. **Contain:** revoke tokens/sessions, disable the affected endpoint, rotate secrets, isolate the service.
3. **Eradicate:** patch the root cause, redeploy, verify.
4. **Recover:** restore from a clean backup if integrity is in doubt; monitor.
5. **Notify:** follow legal/contractual obligations; do not speculate publicly.
6. **Learn:** blameless postmortem, add a regression test, update this document.

For a suspected credential leak: rotate DB/Redis/Kafka/Kong/Grafana credentials, invalidate all refresh tokens, force password resets if hashes may have leaked, and review access logs.

---

## 11. Known limitations (until fixed)

- Single refresh token per user (no multi-device sessions).
- No MFA, account lockout, or breached-password check.
- No audit log or SIEM integration.
- No key rotation/JWKS.
- No mTLS between services.
- No WAF or DDoS protection.
- No formal vulnerability disclosure process (add `SECURITY.md` at repo root).

These are tracked in the audit and roadmap; do not represent them as implemented.

---

## 12. Reporting a vulnerability

Until a root `SECURITY.md` exists, report privately to the repository owner (see `README.md` author section). Do not open a public issue with exploit details. Include affected version/commit, reproduction steps, impact, and any suggested fix. Expect acknowledgment and a remediation timeline.
