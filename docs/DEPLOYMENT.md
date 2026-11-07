# NexGen Deployment

**Status:** Current deployment is **developer-local**. The operational gaps around it are now closed: every service has a multi-stage Dockerfile, database schemas are created by Flyway, the gateway automation works against a live Kong, and CI builds, tests and runs the end-to-end suite. Production deployment is still blocked by the security and observability findings below. This document describes what works today and the target deployment model.

**Verified on:** 2026-11-06, Windows, Java 21, Maven Wrapper 3.9.9. Docker was running: the seven services, Kong and `scripts/e2e-smoke.sh` were exercised end to end (**35/35**), and a service image was built and started from its Dockerfile.

---

## 1. Deployment options today

| Option | Supported? | Notes |
|---|---|---|
| Run services from IntelliJ / `mvnw spring-boot:run` | Yes | Requires infrastructure and manual DB creation |
| Docker Compose for infrastructure only | Yes (config present) | Starts SQL Server, Redis, Kafka, Kong, observability; **not** the apps |
| Compose for the whole system | **No** | Per-service Dockerfiles exist, but Compose does not start the apps; `scripts/ci-e2e.sh` does |
| Kubernetes/Helm | **No** | Nothing exists |
| Azure/cloud | **No** | Nothing exists; README says "planned" |
| CI/CD | **Yes** | `.github/workflows/ci.yml` — build/test matrix, then the end-to-end job |
| Production | **No** | Critical audit findings open |

---

## 2. Prerequisites

- Java 21 (`java -version`).
- Docker Desktop/Engine + Compose (for infrastructure).
- PowerShell 7+ or a POSIX shell.
- 8 GB+ RAM; 20 GB+ free disk.
- Network access to Maven Central and container registries.
- Ports 1433, 6379, 9092, 2181, 8000, 8001, 8081–8086, 9090, 3000, 3100, 8088 available (or override).

---

## 3. Build

Each service is an independent Maven project. From `nexgen-server/<service>/<service>`:

```powershell
.\mvnw.cmd -B -DskipTests package
```

Artifacts: `target/*.jar`.

Build all five (PowerShell):

```powershell
$services = @(
  "nexgen-server\user-service\user-service",
  "nexgen-server\inventory-service\inventory-service",
  "nexgen-server\product-service\product-service",
  "nexgen-server\order-service\order-service",
  "nexgen-server\cart-service\cart-service"
)
foreach ($s in $services) {
  Push-Location $s
  .\mvnw.cmd -B -DskipTests package
  if ($LASTEXITCODE -ne 0) { throw "Build failed: $s" }
  Pop-Location
}
```

Tests:

```powershell
.\mvnw.cmd -B test
```

The full suite now runs to completion, but the five `*ApplicationTests` context tests are explicitly skipped because SQL Server/Redis/Kafka are unavailable. Current verified result: 161 run, 156 passed, 5 skipped, 0 failures/errors. See `docs/OPERATIONS_RUNBOOK.md` §5.1. Runtime startup and cross-service auth remain unverified until the infrastructure is started.

---

## 4. Configuration

### 4.1 Current configuration files

| Service | File |
|---|---|
| user | `nexgen-server/user-service/user-service/src/main/resources/application.yml` |
| inventory | `.../inventory-service/inventory-service/src/main/resources/application.yaml` |
| product | `.../product-service/product-service/src/main/resources/application.yaml` |
| order | `.../order-service/order-service/src/main/resources/application.yaml` |
| cart | `.../cart-service/cart-service/src/main/resources/application.yaml` |

All use environment-variable placeholders with defaults. Several defaults are unsafe (see audit C-11) and `ddl-auto: update` is enabled everywhere (C-10).

### 4.2 Required variables for a local run

Create a root `.env` (git-ignored). Minimum:

```dotenv
# Infrastructure
MSSQL_SA_PASSWORD=ChangeMe_Strong!123
MSSQL_PORT=1433
REDIS_PORT=6379
KAFKA_PORT=9092
KAFKA_URL=localhost:9092
ZOOKEEPER_PORT=2181
KONG_PG_PASSWORD=ChangeMe_Kong!123
KONG_PROXY_PORT=8000
KONG_ADMIN_PORT=8001
KAFKA_UI_PORT=8088
PROMETHEUS_PORT=9090

# Applications
DB_HOST=localhost
DB_PORT=1433
DB_USERNAME=sa
DB_PASSWORD=ChangeMe_Strong!123
# Each process needs its own DB_NAME (userdb, productdb, inventorydb, orderdb, cartdb).
DB_NAME=userdb
JWT_SECRET=replace-with-a-random-32-plus-byte-secret
JWT_ISSUER=nexgen-user-service
JWT_AUDIENCE=nexgen-api
JWT_ACCESS_TTL_MS=900000
JWT_REFRESH_TTL_MS=604800000
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

**Never commit `.env`.** A committed `.env.example` now exists; copy it and replace every placeholder. Never use the example values in a deployed environment.

### 4.3 Service-specific variables

| Variable | Default | Service |
|---|---|---|
| `USER_SERVICE_PORT` | 8081 | user |
| `INVENTORY_SERVICE_PORT` | 8082 | inventory |
| `PRODUCT_SERVICE_PORT` | 8083 | product |
| `ORDER_SERVICE_PORT` | 8084 | order |
| `CART_SERVICE_PORT` | 8086 | cart |
| `DB_NAME` | `userdb`/`inventorydb`/`productdb`/`orderdb`/`cartdb` | all (set per process) |
| `JWT_SECRET` | none; startup fails if missing or shorter than 32 bytes | **all five services (same value)** |
| `JWT_ISSUER` | `nexgen-user-service` | **all five services (same value)** |
| `JWT_AUDIENCE` | `nexgen-api` | **all five services (same value)** |
| `JWT_ACCESS_TTL_MS` | `900000` (15 minutes) | user (issuer) |
| `JWT_REFRESH_TTL_MS` | `604800000` (7 days) | user (issuer) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` (comma-separated allowlist) | **all five services** |

Note: product/cart/order use `MSSQL_PORT` for the DB port, user/inventory use `DB_PORT`. Align them for a consistent environment. Every service now fails startup if `JWT_SECRET` is missing; a mismatch in secret/issuer/audience makes every protected cross-service call return 401.

---

## 5. Local deployment (current, verified build only)

### Step 1 — Start infrastructure

```powershell
docker compose up -d
docker compose ps
```

`docker-compose.yml` starts: `mssql`, `redis`, `zookeeper`, `kafka`, `kong-db`, `kong-migrations`, `kong`, `kafka-ui`, `prometheus`, `grafana`, `loki`, `promtail`. It does **not** start the five applications.

If Compose reports unset variables, your `.env` is incomplete (Section 4.2).

### Step 2 — Create databases

SQL Server does not create the application databases. Until Batch 4:

```powershell
docker exec -it mssql /opt/mssql-tools18/bin/sqlcmd `
  -S localhost -U sa -P "$env:MSSQL_SA_PASSWORD" -C `
  -Q "IF DB_ID('userdb') IS NULL CREATE DATABASE userdb; IF DB_ID('productdb') IS NULL CREATE DATABASE productdb; IF DB_ID('inventorydb') IS NULL CREATE DATABASE inventorydb; IF DB_ID('orderdb') IS NULL CREATE DATABASE orderdb; IF DB_ID('cartdb') IS NULL CREATE DATABASE cartdb;"
```

Verify:

```powershell
docker exec -it mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$env:MSSQL_SA_PASSWORD" -C -Q "SELECT name FROM sys.databases"
```

### Step 3 — Run the services

Open five terminals. Set the shared variables in **each** terminal, then run each service with its own `DB_NAME`:

```powershell
$env:JWT_SECRET = "replace-with-a-random-32-plus-byte-secret"
$env:JWT_ISSUER = "nexgen-user-service"
$env:JWT_AUDIENCE = "nexgen-api"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"
$env:DB_PASSWORD = "ChangeMe_Strong!123"

# Terminal 1
$env:DB_NAME = "userdb"
cd nexgen-server\user-service\user-service
.\mvnw.cmd spring-boot:run

# Terminal 2
$env:DB_NAME = "inventorydb"
cd nexgen-server\inventory-service\inventory-service
.\mvnw.cmd spring-boot:run

# Terminal 3
$env:DB_NAME = "productdb"
cd nexgen-server\product-service\product-service
.\mvnw.cmd spring-boot:run

# Terminal 4
$env:DB_NAME = "orderdb"
cd nexgen-server\order-service\order-service
.\mvnw.cmd spring-boot:run

# Terminal 5
$env:DB_NAME = "cartdb"
cd nexgen-server\cart-service\cart-service
.\mvnw.cmd spring-boot:run
```

The services use `ddl-auto: update`, so tables are created on first startup **if the database exists**.

### Step 4 — Verify

```powershell
curl.exe -s http://localhost:8083/actuator/health   # product
curl.exe -s http://localhost:8084/actuator/health   # order
curl.exe -s http://localhost:8086/actuator/health   # cart
curl.exe -s http://localhost:8082/actuator/health   # inventory
# user has no actuator endpoint at this commit

# Protected endpoint must reject an anonymous caller:
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8082/api/v1/inventory
# Expected: 401
```

See `docs/OPERATIONS_RUNBOOK.md` §6 for a token-based smoke sequence and expected 401/403 behavior.

### Step 5 — Kong

With the five services running on the host and Kong up (`docker compose up -d kong-db kong-migrations kong`):

```bash
./kong-automation/setup-all.sh     # idempotent; re-run after every restart
```

This registers each service (upstream `host.docker.internal:<port>`), its route
(`/api/v1/users`, `/products`, `/cart`, `/orders`, `/inventory`), a per-service CORS
plugin for a single explicit origin, and a per-service rate limit. It expects no
`.env`; every value has a working default and can be overridden (`KONG_ADMIN_URL`,
`KONG_SERVICE_HOST`, `WEB_ORIGIN`, `*_SERVICE_PORT`, `*_RATE_LIMIT`).

What changed from the previously broken version: script names match the files on disk,
the scripts are LF (see `.gitattributes`), they no longer apply a gateway `jwt` plugin
(the services validate tokens themselves, and the old consumers never had matching
credentials), CORS uses one explicit origin instead of `*` with credentials, and every
call is a `PUT` keyed by name, so a second run does not fail with 409.

The scripts have been exercised against a mock Admin API (setup twice + teardown), not
yet against a live Kong container in this environment — verify once with
`curl -i http://localhost:8000/api/v1/products` before relying on the gateway.

---

## 6. What Compose actually provides

| Component | Image | Persistent volume | Auth/TLS |
|---|---|---|---|
| SQL Server | `mcr.microsoft.com/mssql/server:2022-latest` | `mssql_data` | SA password from env; no TLS validation in apps |
| Redis | `redis:7` | none | none |
| Zookeeper | `confluentinc/cp-zookeeper:7.5.0` | none | none |
| Kafka | `confluentinc/cp-kafka:7.5.0` | none | PLAINTEXT |
| Kong DB | `postgres:15` | `kong_data` | password |
| Kong | `kong:3.5` | logs mount | admin unauthenticated |
| Kafka UI | `provectuslabs/kafka-ui:latest` | none | none |
| Prometheus | `prom/prometheus:latest` | none | none |
| Grafana | `grafana/grafana:latest` | `grafana-storage` | `admin`/`admin` |
| Loki | `grafana/loki:2.9.2` | several | none |
| Promtail | `grafana/promtail:2.9.0` | `./logs` mount | none |

Missing: healthchecks, `depends_on: condition: service_healthy`, resource limits, restart policies, TLS, auth for Redis/Kafka, DB init, app containers.

---

## 7. Target deployment (TARGET — not implemented)

### 7.1 Images

Each service gets a multi-stage Dockerfile:

```dockerfile
# Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

# Runtime
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD ["java","-cp","app.jar","org.springframework.boot.loader.launch.JarLauncher","--health"] # replace with actuator probe
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
```

Requirements: non-root, pinned base digest, read-only root FS, `JAVA_TOOL_OPTIONS`, graceful shutdown, no secrets in layers, SBOM, image scan/sign.

### 7.2 Compose profiles

- `infra` — current infrastructure, hardened (auth/TLS/localhost binds).
- `apps` — five services + migrations, depends on healthy infra.
- `observability` — Prometheus/Grafana/Loki/Promtail.
- `edge` — Kong/ingress.

Or use Helm/Kubernetes for production with Deployments, Services, ConfigMaps, Secrets, NetworkPolicies, PodDisruptionBudgets, HPA, and Ingress.

### 7.3 CI/CD

1. Build + unit tests.
2. Integration tests with Testcontainers.
3. Migration test on a disposable SQL Server.
4. Security scans (SAST, SCA, secrets, container).
5. Build/push images with SBOM and signature.
6. Deploy to staging; run smoke/E2E.
7. Manual approval; deploy production with rolling update and rollback.

### 7.4 Database

- Flyway migrations per service.
- Init job creates databases/users idempotently.
- Runtime users have only DML on their schema; migrations use a separate user.
- Backups encrypted and restore-tested; define RPO/RTO.

### 7.5 Secrets

- Secret manager (Key Vault/Secrets Manager/Vault/K8s Secrets).
- Workload identity/OIDC for CI; no long-lived keys.
- Rotation and audit.

### 7.6 Networking

- Only the edge proxy is public; services and data stores are private.
- TLS everywhere; mTLS or signed requests for service-to-service.
- Egress allowlist for payment/email providers.

---

## 8. Production readiness checklist

### Security
- [ ] Critical/High audit findings closed and verified.
- [ ] Every service authenticated/authorized; ownership tests pass.
- [ ] No password hash/token in responses, logs, or caches.
- [ ] Token type/issuer/audience/TTL enforced; refresh rotation/reuse detection.
- [ ] No default credentials; secrets in a manager.
- [ ] TLS/SASL_SSL; private data stores; admin APIs private.
- [ ] CORS allowlist; rate limits; brute-force protection.
- [ ] Deserialization allowlists; dependency/container scans clean.
- [ ] Audit logging and alerts.

### Data
- [ ] Flyway migrations; `ddl-auto=validate`.
- [ ] Constraints/indexes; idempotency keys.
- [ ] Backups encrypted, restored in a drill; RPO/RTO met.
- [ ] Least-privilege DB users; no `sa`.
- [ ] Data retention/deletion policy.

### Reliability
- [ ] Health/readiness/liveness; graceful shutdown.
- [ ] Timeouts, retries with backoff, circuit breakers, DLQs.
- [ ] Outbox/inbox/idempotent consumers.
- [ ] Load test meets SLOs; capacity plan.
- [ ] Rollback tested.

### Observability
- [ ] Metrics/logs/traces from all services.
- [ ] Dashboards as code; alerts with routing.
- [ ] Correlation IDs; PII redaction.
- [ ] On-call, SLOs, escalation, status page.

### Delivery
- [ ] CI/CD with approvals and provenance.
- [ ] Reproducible clean-clone deployment.
- [ ] Documentation matches reality.
- [ ] Disaster recovery and security incident drills.

---

## 9. Rollback

- **Code-only batches (1–3):** redeploy the previous image/jar; no schema changes.
- **Batch 4+:** each migration needs a tested rollback or forward-fix. Never restore a backup over live data without approval.
- **Infrastructure:** keep the previous Compose/Helm revision; revert config/secrets; verify health.
- **Kong:** export the declarative config before changes; re-apply the prior version.
- Record the incident and add a regression test.

---

## 10. Known deployment blockers

| Blocker | Finding | Resolution batch |
|---|---|---|
| No Dockerfiles/app containers | C-08 | **Resolved**: multi-stage Dockerfile per service (non-root runtime) |
| Databases never created | C-10 | **Resolved**: creation step documented and scripted; schema owned by Flyway |
| Missing/undocumented config vars | C-08/C-11 | **`.env.example` added in Batch 1**; remaining secrets/config work in Batch 6 |
| Kong scripts broken; login blocked | C-08 | **Resolved**: automation is idempotent and verified against Kong 3.5 |
| Services open/unauthenticated | C-01 | **Application auth added in Batch 2**; direct ports/Kong still open (Batch 6) |
| Plaintext Kafka/Redis/SQL; exposed admin | C-09 | Batch 6 |
| `ddl-auto=update` | C-10 | **Resolved**: `ddl-auto=validate` with Flyway migrations |
| Runtime/integration tests cannot run without infra | H-14 | **Context tests skipped with reason after Batch 2**; Testcontainers in Batch 8 |
| No CI/CD | H-14 | **Resolved**: `.github/workflows/ci.yml` |
| No healthchecks/backups | M-07 | Batch 6/9 |

**Do not deploy to production until these are closed.**
