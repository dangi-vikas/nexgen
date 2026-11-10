#!/usr/bin/env bash
# Full-stack verification for CI (and for any Linux machine):
# infrastructure -> databases -> build -> run the seven services -> Kong -> E2E suite.
#
# Requires: docker with compose, Java 21, bash, curl, node.
#   ./scripts/ci-e2e.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# dir module database port — the jar name is read from target/ after the build, because
# user and product are 1.0.0 while the other five are 0.0.1-SNAPSHOT.
SERVICES=(
  "user-service user-service userdb 8081"
  "product-service product-service productdb 8083"
  "inventory-service inventory-service inventorydb 8082"
  "cart-service cart-service cartdb 8086"
  "order-service order-service orderdb 8084"
  "payment-service payment-service paymentdb 8085"
  "notification-service notification-service notificationdb 8087"
)

# A .env is required by the services and by the E2E script. CI has none, so create the
# development one; an existing .env is never overwritten.
if [ ! -f .env ]; then
  echo "==> writing development .env"
  cat > .env <<'ENV'
DB_HOST=localhost
DB_PORT=1433
DB_USERNAME=sa
DB_PASSWORD=NexGen!Dev2026
MSSQL_SA_PASSWORD=NexGen!Dev2026
MSSQL_PORT=1433
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_HOST=localhost
KAFKA_PORT=9092
KAFKA_URL=localhost:9092
ZOOKEEPER_PORT=2181
KONG_PROXY_PORT=8000
KONG_ADMIN_PORT=8001
KONG_PG_PASSWORD=KongDev2026!
JWT_SECRET=ci-only-secret-0123456789abcdefghijklmnopqrstuvwxyz
JWT_ISSUER=nexgen-user-service
JWT_AUDIENCE=nexgen-api
CORS_ALLOWED_ORIGINS=http://localhost:3000
USER_SERVICE_PORT=8081
PRODUCT_SERVICE_PORT=8083
INVENTORY_SERVICE_PORT=8082
CART_SERVICE_PORT=8086
ORDER_SERVICE_PORT=8084
PAYMENT_SERVICE_PORT=8085
NOTIFICATION_SERVICE_PORT=8087
SMTP_HOST=localhost
SMTP_PORT=1025
MAILPIT_SMTP_PORT=1025
MAILPIT_UI_PORT=8025
NOTIFICATIONS_FROM=no-reply@nexgen.local
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=NexGen!Admin2026
ENV
fi

set -a
# shellcheck disable=SC1091
. ./.env
set +a

echo "==> starting infrastructure"
docker compose up -d mssql redis mailpit zookeeper kafka kong-db

echo "==> waiting for SQL Server"
for _ in $(seq 1 60); do
  if docker exec mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U "$DB_USERNAME" \
      -P "$DB_PASSWORD" -C -Q "SELECT 1" >/dev/null 2>&1; then
    break
  fi
  sleep 5
done

echo "==> creating databases"
for db in userdb productdb inventorydb cartdb orderdb paymentdb notificationdb; do
  docker exec mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U "$DB_USERNAME" -P "$DB_PASSWORD" -C \
    -Q "IF DB_ID('$db') IS NULL CREATE DATABASE $db"
done

echo "==> building services"
mkdir -p logs/e2e
for entry in "${SERVICES[@]}"; do
  read -r dir module db port <<<"$entry"
  ( cd "nexgen-server/$dir/$module" && chmod +x mvnw && ./mvnw -B -DskipTests package )
done

echo "==> starting services"
for entry in "${SERVICES[@]}"; do
  read -r dir module db port <<<"$entry"
  module_dir="nexgen-server/$dir/$module"
  shopt -s nullglob
  jars=("$module_dir"/target/*.jar)
  shopt -u nullglob
  if [ "${#jars[@]}" -ne 1 ]; then
    echo "!! expected exactly one jar in $module_dir/target, found ${#jars[@]}" >&2
    exit 1
  fi
  jar="${jars[0]##*/}"
  echo "    $dir -> $jar"
  # Each service runs from its own module directory: logback writes to ../../../logs/<service>.
  ( cd "$module_dir" && exec env DB_NAME="$db" java -jar "target/$jar" ) \
    >"logs/e2e/$dir.log" 2>&1 &
  echo "$!" >"logs/e2e/$dir.pid"
done

echo "==> waiting for services"
for entry in "${SERVICES[@]}"; do
  read -r dir module db port <<<"$entry"
  code=""
  for _ in $(seq 1 40); do
    code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/actuator/health" || true)
    [ "$code" = "200" ] && break
    sleep 3
  done
  if [ "$code" != "200" ]; then
    echo "!! $dir did not become healthy (last status: ${code:-none})" >&2
    tail -40 "logs/e2e/$dir.log" >&2 || true
    exit 1
  fi
  echo "    $dir healthy on $port"
done

echo "==> starting Kong"
docker compose up -d kong
for _ in $(seq 1 30); do
  if curl -fsS "http://localhost:${KONG_ADMIN_PORT:-8001}/status" >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

echo "==> configuring Kong"
./kong-automation/setup-all.sh

echo "==> running end-to-end suite"
./scripts/e2e-smoke.sh
