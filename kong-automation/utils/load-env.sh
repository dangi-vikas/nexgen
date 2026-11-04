#!/usr/bin/env bash
# Shared configuration for the Kong admin scripts.
#
# Every value has a default that matches docker-compose.yml and the service ports, so
# the scripts work from a clean checkout with no .env at all. If a .env exists at the
# repository root it is loaded and overrides those defaults.
set -euo pipefail

KONG_UTILS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KONG_DIR="$(cd "$KONG_UTILS_DIR/.." && pwd)"
REPO_ROOT="$(cd "$KONG_DIR/.." && pwd)"

if [ -f "$REPO_ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$REPO_ROOT/.env"
  set +a
fi

KONG_ADMIN_PORT="${KONG_ADMIN_PORT:-8001}"
KONG_ADMIN_URL="${KONG_ADMIN_URL:-http://localhost:${KONG_ADMIN_PORT}}"

# Kong runs in a container while the Spring services run on the host, so an upstream
# of "localhost:<port>" would point at the Kong container itself.
KONG_SERVICE_HOST="${KONG_SERVICE_HOST:-host.docker.internal}"
WEB_ORIGIN="${WEB_ORIGIN:-http://localhost:3000}"

USER_SERVICE_PORT="${USER_SERVICE_PORT:-8081}"
PRODUCT_SERVICE_PORT="${PRODUCT_SERVICE_PORT:-8083}"
CART_SERVICE_PORT="${CART_SERVICE_PORT:-8086}"
ORDER_SERVICE_PORT="${ORDER_SERVICE_PORT:-8084}"
INVENTORY_SERVICE_PORT="${INVENTORY_SERVICE_PORT:-8082}"

USER_SERVICE_NAME="${USER_SERVICE_NAME:-user-service}"
PRODUCT_SERVICE_NAME="${PRODUCT_SERVICE_NAME:-product-service}"
CART_SERVICE_NAME="${CART_SERVICE_NAME:-cart-service}"
ORDER_SERVICE_NAME="${ORDER_SERVICE_NAME:-order-service}"
INVENTORY_SERVICE_NAME="${INVENTORY_SERVICE_NAME:-inventory-service}"

USER_SERVICE_URL="${USER_SERVICE_URL:-http://${KONG_SERVICE_HOST}:${USER_SERVICE_PORT}}"
PRODUCT_SERVICE_URL="${PRODUCT_SERVICE_URL:-http://${KONG_SERVICE_HOST}:${PRODUCT_SERVICE_PORT}}"
CART_SERVICE_URL="${CART_SERVICE_URL:-http://${KONG_SERVICE_HOST}:${CART_SERVICE_PORT}}"
ORDER_SERVICE_URL="${ORDER_SERVICE_URL:-http://${KONG_SERVICE_HOST}:${ORDER_SERVICE_PORT}}"
INVENTORY_SERVICE_URL="${INVENTORY_SERVICE_URL:-http://${KONG_SERVICE_HOST}:${INVENTORY_SERVICE_PORT}}"

# Route prefixes are the services' own controller mappings; they are forwarded as-is
# (strip_path=false), so nothing depends on the gateway rewriting URLs.
USER_SERVICE_ROUTE="${USER_SERVICE_ROUTE:-/api/v1/users}"
PRODUCT_SERVICE_ROUTE="${PRODUCT_SERVICE_ROUTE:-/api/v1/products}"
CART_SERVICE_ROUTE="${CART_SERVICE_ROUTE:-/api/v1/cart}"
ORDER_SERVICE_ROUTE="${ORDER_SERVICE_ROUTE:-/api/v1/orders}"
INVENTORY_SERVICE_ROUTE="${INVENTORY_SERVICE_ROUTE:-/api/v1/inventory}"

# Requests per minute per service. Sign-in is deliberately lower than catalogue reads.
USER_RATE_LIMIT="${USER_RATE_LIMIT:-120}"
PRODUCT_RATE_LIMIT="${PRODUCT_RATE_LIMIT:-600}"
CART_RATE_LIMIT="${CART_RATE_LIMIT:-300}"
ORDER_RATE_LIMIT="${ORDER_RATE_LIMIT:-300}"
INVENTORY_RATE_LIMIT="${INVENTORY_RATE_LIMIT:-300}"
