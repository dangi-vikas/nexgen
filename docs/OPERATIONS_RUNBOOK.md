# NexGen Operations Runbook

**Status:** Current-state runbook for a local/dev deployment at commit `bf41b37`, updated for the uncommitted Batch 1 and Batch 2 working-tree changes. It is not a production runbook yet. Production sections are marked **TARGET** and are not implemented.

**Audience:** Developers and operators running or debugging NexGen.  
**Related:** `docs/DEPLOYMENT.md`, `docs/PRODUCTION_READINESS_AUDIT.md`.

---

## 1. What runs

| Component | Port | Health endpoint | Notes |
|---|---:|---|---|
| user-service | 8081 | **none** (Actuator dependency missing) | Token issuer; requires `JWT_SECRET`; login/register public |
| inventory-service | 8082 | `/actuator/health` | Resource server; reads require a token, writes require admin/internal; health is public |
| product-service | 8083 | `/actuator/health` | Resource server; product reads public, writes admin-only; health is public |
| order-service | 8084 | `/actuator/health` | Resource server; owner/privileged order access; health is public |
| cart-service | 8086 | `/actuator/health` | Resource server; self-only cart; health is public |
| Kong proxy | 8000 | `/api/v1/products` | Routes registered by `kong-automation/setup-all.sh`; proxied request not yet verified live |
| Kong admin | 8001 | `/` | **Unauthenticated, do not expose** |
| SQL Server | 1433 | `SELECT 1` | `sa` only by default; databases not created |
| Redis | 6379 | `PING` | No auth |
| Kafka | 9092 | broker API | PLAINTEXT |
| Prometheus | 9090 | `/-/healthy` | Scrapes order/cart only |
| Grafana | 3000 | `/api/health` | `admin`/`admin` |
| Loki | 3100 | `/ready` | No auth |
| Kafka UI | 8080 (host var) | `/` | No auth |

> Ports are configurable through environment variables (see `docs/DEPLOYMENT.md`). The values above are the defaults.

---

## 2. Prerequisites

- Java 21 (the projects target Java 17 bytecode; Java 21 is verified).
- Maven Wrapper 3.9.9 (committed; no global Maven required).
- Docker + Docker Compose for infrastructure.
- PowerShell 7+ on Windows (commands below) or a POSIX shell.
- At least 8 GB RAM for SQL Server + Kafka + Kong + observability.
- Network access to Maven Central on first build.

**Verified audit environment:** Java 21, Maven Wrapper 3.9.9, Windows, **Docker unavailable**. Container commands below were not executed during the audit.

---

## 3. Environment variables

A committed `.env.example` exists at the repository root. Copy it to `.env`, replace every placeholder, and keep `.env` git-ignored. Do not use the example values outside local development.

| Variable | Purpose | Default/current behavior | Required |
|---|---|---|---|
| `MSSQL_SA_PASSWORD` | SQL Server SA password for Compose | none; Compose fails if unset | Yes (dev) |
| `MSSQL_PORT` | SQL Server host port | `${MSSQL_PORT}` with no default in Compose | Yes |
| `DB_HOST` | SQL Server host | `localhost` | No |
| `DB_PORT` | SQL Server port used by user-service | `1433` | No |
| `DB_USERNAME` | DB user | `sa` | No (change for prod) |
| `DB_PASSWORD` | DB password | **hardcoded fallback** `YourStrong@Passw0rd` in user/product; required in inventory/order/cart | Yes (remove fallback in Batch 4/6) |
| `DB_NAME` | Database name | per service (`userdb`, etc.) | No |
| `REDIS_HOST` / `REDIS_PORT` | Redis | `localhost:6379` | No |
| `KAFKA_HOST` / `KAFKA_PORT` | Kafka bootstrap | `localhost:9092` | No |
| `KAFKA_URL` | Kafka advertised listener | none; Compose produces `PLAINTEXT://` if unset | Yes (dev) |
| `ZOOKEEPER_PORT` | Zookeeper host port | none | Yes |
| `JWT_SECRET` | HS256 signing secret, shared by all five services | none; startup fails if missing or <32 bytes | Yes |
| `JWT_ISSUER` | JWT issuer claim | `nexgen-user-service` | No (must match) |
| `JWT_AUDIENCE` | JWT audience claim | `nexgen-api` | No (must match) |
| `JWT_ACCESS_TTL_MS` | Access token lifetime | `900000` (15 min) | No |
| `JWT_REFRESH_TTL_MS` | Refresh token lifetime | `604800000` (7 days) | No |
| `CORS_ALLOWED_ORIGINS` | Comma-separated CORS allowlist | `http://localhost:3000` | No (set per environment) |
| `KONG_PG_PASSWORD` | Kong Postgres password | none | Yes (dev) |
| `KONG_PROXY_PORT` / `KONG_ADMIN_PORT` | Kong ports | none | Yes (dev) |
| `KAFKA_UI_PORT` | Kafka UI host port | none | Yes (dev) |
| `PROMETHEUS_PORT` | Prometheus host port | `9090` | No |
| `USER_SERVICE_PORT` etc. | Service ports | 8081/8082/8083/8084/8086 | No |

**Current startup blockers:** missing `.env`, missing databases, and the broken Kong automation. `JWT_SECRET` is required by all five services and is validated at startup. Each service must receive the same issuer/audience values; otherwise every protected request returns 401.

---

## 4. Start infrastructure (current workaround)

1. Copy the checked-in template and edit it:

```powershell
Copy-Item .env.example .env
# Replace every placeholder. Use a random JWT_SECRET of at least 32 bytes.
```

Minimum user-service values:

```dotenv
MSSQL_SA_PASSWORD=ChangeMe_Strong!123
MSSQL_PORT=1433
KAFKA_URL=localhost:9092
KAFKA_PORT=9092
ZOOKEEPER_PORT=2181
KONG_PG_PASSWORD=ChangeMe_Kong!123
KONG_PROXY_PORT=8000
KONG_ADMIN_PORT=8001
KAFKA_UI_PORT=8088
PROMETHEUS_PORT=9090
JWT_SECRET=replace-with-a-random-32-plus-byte-secret
JWT_ISSUER=nexgen-user-service
JWT_AUDIENCE=nexgen-api
JWT_ACCESS_TTL_MS=900000
JWT_REFRESH_TTL_MS=604800000
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

2. Start the stack:

```powershell
docker compose up -d
docker compose ps
```

3. **Create the databases.** The repository does not do this. Until Batch 4 adds an init job, run:

```powershell
docker exec -it mssql /opt/mssql-tools18/bin/sqlcmd `
  -S localhost -U sa -P "$env:MSSQL_SA_PASSWORD" -C `
  -Q "IF DB_ID('userdb') IS NULL CREATE DATABASE userdb; IF DB_ID('productdb') IS NULL CREATE DATABASE productdb; IF DB_ID('inventorydb') IS NULL CREATE DATABASE inventorydb; IF DB_ID('orderdb') IS NULL CREATE DATABASE orderdb; IF DB_ID('cartdb') IS NULL CREATE DATABASE cartdb;"
```

If `sqlcmd` is not at that path, use `/opt/mssql-tools/bin/sqlcmd` or install `mssql-tools18` in the container.

4. Verify infrastructure:

```powershell
docker exec -it redis redis-cli PING
docker logs --tail 50 kafka
docker logs --tail 50 kong
```

> `kong-automation/setup-all.sh` is idempotent and safe to re-run. It registers the five public services, their routes and the per-service CORS/rate-limit plugins, and it does **not** enable a gateway `jwt` plugin (the services validate tokens themselves). Run it after Kong is up and the services are listening on the host; verify with `curl -i http://localhost:8000/api/v1/products`.

---

## 5. Build and run services

From each service directory (`nexgen-server/<service>/<service>`):

```powershell
.\mvnw.cmd -B -DskipTests package
$env:JWT_SECRET = "replace-with-a-random-32-plus-byte-secret"
$env:JWT_ISSUER = "nexgen-user-service"
$env:JWT_AUDIENCE = "nexgen-api"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"
$env:DB_PASSWORD = "ChangeMe_Strong!123"
# Set DB_NAME to the database owned by this service (userdb, productdb, inventorydb, orderdb, cartdb).
$env:DB_NAME = "userdb"
.\mvnw.cmd spring-boot:run
```

Run each service in a separate terminal. Every service must receive the same `JWT_SECRET`, `JWT_ISSUER`, and `JWT_AUDIENCE`. Startup order is not enforced; start SQL/Redis/Kafka first. The services do not retry indefinitely on DB failure, so start infrastructure before the apps.

### 5.1 Verified build and isolated-test baseline

```text
user-service        54 run / 53 passed / 1 skipped
inventory-service   25 run / 24 passed / 1 skipped
product-service     26 run / 25 passed / 1 skipped
order-service       33 run / 32 passed / 1 skipped
cart-service        23 run / 22 passed / 1 skipped
--------------------------------------------------
total               161 run / 156 passed / 5 skipped / 0 failures / 0 errors
```

All five `mvnw -B -DskipTests package` builds succeed. The five skips are `*ApplicationTests` context tests, disabled because SQL Server, Redis, and Kafka are unavailable. No runtime smoke test has been executed yet; Docker was intentionally not started.

---

## 6. Smoke checks

Replace `{...}` with real values. The examples assume a local user named `smoke`.

```powershell
# Registration (returns UserProfileResponse; no password/hash)
curl.exe -s -X POST http://localhost:8081/api/v1/users/register `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"smoke\",\"password\":\"SmokeTest!12345\",\"email\":\"smoke@example.com\"}'

# Login and capture the access token
$auth = curl.exe -s -X POST http://localhost:8081/api/v1/users/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"smoke\",\"password\":\"SmokeTest!12345\"}' | ConvertFrom-Json
$token = $auth.accessToken

# Public product list (no token)
curl.exe -s http://localhost:8083/api/v1/products

# Protected inventory read (requires a token)
curl.exe -s -H "Authorization: Bearer $token" http://localhost:8082/api/v1/inventory

# Self-only cart read
curl.exe -s -H "Authorization: Bearer $token" http://localhost:8086/api/v1/cart/smoke

# Order creation (userId is taken from the token, not the body)
curl.exe -s -X POST http://localhost:8084/api/v1/orders `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{\"orderItems\":[{\"skuCode\":\"SKU123\",\"quantity\":1,\"price\":10.0}]}'

# Public health endpoints
curl.exe -s http://localhost:8083/actuator/health
```

Expected current behavior (after Batch 2):
- Registration returns a `UserProfileResponse` with **no password/hash**.
- Login returns an access token and a refresh token with distinct token types; the access token expires in 15 minutes.
- The product list and health endpoints work without a token.
- Inventory/cart/order calls require a valid bearer token and return JSON 401 without one.
- A user token cannot write products/inventory (403), cannot read another user's cart (403), and cannot update order status (403).
- A refresh token sent as a bearer token to a protected endpoint returns 401. An access token sent to `/refresh-token` returns 401.
- The product list is intentionally public; all other business endpoints require authentication.
- Checkout still returns simulated success and trusts client prices; do **not** use it for a real transaction.

---

## 7. Logs

Each service writes a JSON-ish file to a path relative to its working directory:

```
<repo>/logs/<service>/application.log
```

Example: `C:\Users\kprsa\Projects\nexgen\logs\user-service\application.log`.

Current limitations:
- No console appender, so `docker logs` is empty if the app runs in a container.
- No rotation or size cap; files grow without bound.
- Path is relative to CWD; running from another directory changes the location.
- Promtail watches only `cart-service` and Kong.
- Hibernate SQL DEBUG is enabled.

Useful commands:

```powershell
Get-Content .\logs\user-service\application.log -Tail 200 -Wait
Get-ChildItem .\logs -Recurse | Sort-Object Length -Descending | Select-Object -First 10 FullName,Length
```

---

## 8. Metrics and dashboards

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (`admin`/`admin`)
- Loki: http://localhost:3100
- Kafka UI: http://localhost:${KAFKA_UI_PORT}

Current scrape targets (`prometheus/prometheus.yml`): all seven services plus Prometheus itself, addressed as `host.docker.internal:8081` (user), `8082` (inventory), `8083` (product), `8084` (order), `8085` (payment), `8086` (cart), `8087` (notification) and `localhost:9090`. The `prometheus` Compose service sets `extra_hosts: ["host.docker.internal:host-gateway"]`, so the targets resolve on Linux as well as Docker Desktop.

Each service must leave `/actuator/prometheus` unauthenticated or the scrape returns 401 and the target shows as down; the five services with Spring Security (user, product, inventory, order, cart) list it in `SecurityConfig` alongside `health` and `info`. Check a single target by hand with `curl -s localhost:8083/actuator/prometheus | head`.

Useful PromQL:

```promql
up
rate(http_server_requests_seconds_count[5m])
sum by (status) (rate(order_status_updated_total[5m]))
```

Grafana is provisioned from the repository: `monitoring/grafana/provisioning` defines the Prometheus and Loki datasources (fixed uids, so panels never need remapping) and `monitoring/grafana/dashboards/nexgen-saga.json` is loaded as the **NexGen — Services & Checkout Saga** dashboard. A dashboard edited in the UI is lost on the next container recreate; edit the JSON and commit it instead. The host port is `GRAFANA_PORT` (default `3000`), which is `3001` in the local `.env` because 3000 is taken on the development machine.

Log shipping: Promtail's position file is `/var/logs/.promtail-positions.yaml`, inside the mounted `./logs` directory. That matters — with the position file in the container's `/tmp`, every recreate re-sent the entire log history, and Loki answered those catch-up batches with `429 Too Many Requests` (`Ingestion rate limit exceeded ... limit: 4194304 bytes/sec`) while dropping the lines. Loki now allows `ingestion_rate_mb: 16` with a `32` MB burst, and Promtail sends 256 KB batches every second. If log lines go missing after a restart, check `docker logs promtail | grep 429` first and confirm the positions file exists.

### Alerting

Four rules are provisioned from `monitoring/grafana/provisioning/alerting/alert-rules.yml` into the **NexGen** folder, evaluated every minute:

| Rule | Condition | For | Severity |
|---|---|---|---|
| NexGen service down | `up{job=~".+-service"}` below 1 | 2m | critical |
| NexGen 5xx response ratio | 5xx share of requests > 5% | 5m | warning |
| NexGen p95 latency | p95 request latency > 1.5s | 10m | warning |
| NexGen database connections queued | `hikaricp_connections_pending` > 5 | 5m | warning |

Delivery is `contact-points.yml` (e-mail to `alerts@nexgen.local`) driven through Grafana's SMTP settings, which point at Mailpit. Read fired alerts at http://localhost:8025, or confirm the tree with `curl -u admin:admin localhost:3001/api/v1/provisioning/alert-rules`. Critical alerts bypass the 30s group wait; everything repeats every four hours (one hour for critical).

To prove delivery end to end, stop one service and wait about three minutes:

```bash
taskkill //F //PID <notification-service pid>   # or stop it however you started it
sleep 200 && curl -s 'localhost:8025/api/v1/messages?limit=5' | grep -o '"Subject":"[^"]*"'
```

Editing a rule in the UI is lost on the next Grafana restart: change the YAML and recreate the container.

---

## 9. Common failure modes

| Symptom | Likely cause | Diagnosis | Action |
|---|---|---|---|
| App fails at startup: `JWT secret must be at least 32 bytes` | `JWT_SECRET` unset/short | Check env | Set a random ≥32-byte secret; changing it invalidates all tokens |
| `Cannot open database "userdb" requested by the login` | DB not created | `sqlcmd -Q "SELECT name FROM sys.databases"` | Create databases (Section 4) |
| `Connection refused` to SQL Server | Container not up or wrong port | `docker compose ps`, `Test-NetConnection localhost -Port 1433` | Start MSSQL; fix `.env` |
| Redis connection errors | Redis down or wrong host/port | `docker exec redis redis-cli PING` | Start Redis; check `REDIS_HOST/PORT` |
| Kafka producer timeouts | Kafka unhealthy or advertised listener wrong | `docker logs kafka`, `kafka-topics --list` | Fix `KAFKA_URL`; ensure `localhost:9092` is advertised for host clients |
| 401 on a previously valid token | Token expired, revoked, or signed with another secret | Check `exp`/login time and user-service logs | Refresh or re-login; do not change `JWT_SECRET` casually |
| 401 on every protected service after login works | `JWT_SECRET`, issuer, or audience differs between user-service and the target service | Compare env values and service startup config | Make all five services use the same `JWT_SECRET`/`JWT_ISSUER`/`JWT_AUDIENCE` |
| 403 with a valid token | Role missing or ownership mismatch | Decode the token roles and compare the path/body `userId` with `sub` | Use the correct account/role; admin/internal provisioning is not implemented yet |
| 401 on `/refresh-token` after a successful login | Token already rotated/reused or Redis state was deleted | Check Redis `refresh_token:<username>` and logs | Re-login; treat repeated replay as a possible compromise |
| 503 `AUTH_UNAVAILABLE` | Redis unavailable or failing | `docker exec redis redis-cli PING`, logs | Restore Redis; authentication fails closed |
| Login works directly but not through Kong | Kong JWT plugin applied to login/register | Kong logs | Do not use Kong scripts until Batch 6 |
| `docker compose` warns about unset variables | Missing `.env` | Compose output | Create `.env` from `.env.example` (Section 3) |
| Port already in use | Another service/container | `Get-NetTCPConnection -LocalPort 8081` | Stop the conflicting process or change the port |
| Grafana login fails | Default credentials changed/volume state | `docker logs grafana` | Reset the admin password or remove the volume (dev only) |
| Logs not in Loki | Promtail only watches cart/Kong; path mismatch | `docker logs promtail` | Expected; fix in Batch 7 |

---

## 10. Incident procedures (current)

### 10.1 Authentication failure spike
1. Check user-service logs for `JwtException`, DB errors, and Redis errors.
2. Verify `JWT_SECRET` has not changed (changing it invalidates all tokens).
3. Verify Redis is reachable; blacklist/refresh checks depend on it.
4. If compromised, rotate the secret, flush refresh tokens, and force re-login.
5. **Gap:** no audit log or alert exists yet; add in Batch 7.

### 10.2 5xx spike
1. Check which service and endpoint via logs/metrics.
2. Check SQL Server, Redis, and Kafka connectivity.
3. Check disk space and log file size.
4. Authentication failures should return 401/503, not 500; if you see 500 on a token path, capture the stack trace and treat it as a regression.
5. Roll back the last deployment if correlated.

### 10.3 Kafka lag / DLQ growth
- No consumer lag alert or DLQ exists today.
- Inspect topics with Kafka UI or `kafka-consumer-groups --describe`.
- Check producer logs for send failures.
- Do not delete topics or consumer groups in production without a runbook and approval.

### 10.4 Database down
1. Stop accepting writes at the edge if possible.
2. Restore SQL Server connectivity; do not run `ddl-auto=update` on a partially available DB.
3. Verify data integrity before resuming.
4. **Gap:** no backup/restore automation; add in Batch 6/9.

### 10.5 Redis down
- Authentication blacklist/refresh checks fail; requests may return 500.
- Cart/user caches are unavailable; DB remains the source of truth.
- Restart Redis; expect cache cold-start. Do not flush data blindly.

### 10.6 Disk pressure
- Log files are unbounded. Identify large files under `logs/`.
- Truncate only after preserving evidence; add rotation in Batch 7.
- Check Docker volumes (`docker system df`).

---

## 11. Backup and restore (TARGET — not implemented)

Current state: no backup job, retention policy, or restore test.

Target procedure for SQL Server:

```powershell
# Backup (example; use a secure path and encryption)
docker exec mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$env:MSSQL_SA_PASSWORD" -C `
  -Q "BACKUP DATABASE userdb TO DISK='/var/opt/mssql/backup/userdb.bak' WITH INIT, COMPRESSION;"

# Copy out
docker cp mssql:/var/opt/mssql/backup/userdb.bak .\backups\

# Restore (example; stop writers first)
docker exec mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$env:MSSQL_SA_PASSWORD" -C `
  -Q "RESTORE DATABASE userdb FROM DISK='/var/opt/mssql/backup/userdb.bak' WITH REPLACE;"
```

Requirements: encrypted backups, off-host storage, retention, restore drills, measured RTO/RPO. Define RPO/RTO before production.

---

## 12. Maintenance

- **Dependency updates:** monthly, with tests; security patches as soon as practical.
- **Secret rotation:** on schedule and on suspicion; coordinate JWT rotation to avoid mass logout (or accept it).
- **Schema changes:** Flyway migrations only (after Batch 4); review and test on a copy.
- **Certificate renewal:** automate and alert before expiry (target).
- **Capacity:** monitor DB size, Redis memory, Kafka disk, log disk; scale before limits.
- **Cleanup:** prune old images/volumes only after confirming backups.

---

## 13. Rollback

1. Stop the new version; keep the old image/jar available.
2. If a DB migration is involved, follow its documented down/forward-fix plan. Do not restore over data without approval.
3. Revert configuration/secrets to the previous known-good set.
4. Verify health and a smoke transaction.
5. Record the incident and add a regression test.

Current code-only batches (1–3) are rollback-safe by redeploying the previous build. Batch 4 introduces migrations; each must have a tested rollback or forward-fix strategy.

---

## 14. Escalation and ownership

No on-call rotation, SLOs, or escalation policy exists in the repository. Before production, define:

- Service owner and backup owner.
- Severity levels and response times.
- Paging channel and escalation path.
- Status page/customer communication.
- Vendor contacts (cloud, payment, SMS/email).

**Production readiness:** NO-GO until the Critical audit findings are closed and the deployment/observability batches are complete.
