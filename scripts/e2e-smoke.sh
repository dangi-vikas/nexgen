#!/usr/bin/env bash
# End-to-end smoke test: gateway -> services -> Kafka saga -> payment -> notification.
#
# Prerequisites: `docker compose up -d mssql redis mailpit zookeeper kafka kong-db kong`,
# the seven services running on the host, and kong-automation/setup-all.sh already run.
#
#   ./scripts/e2e-smoke.sh
#
# Exits non-zero on the first failed assertion and prints a summary of every check.
set -uo pipefail
# Git Bash rewrites /opt/... arguments into Windows paths, which breaks `docker exec`.
export MSYS_NO_PATHCONV=1

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
set -a; . "$REPO_ROOT/.env"; set +a

GATEWAY="http://localhost:${KONG_PROXY_PORT:-8000}"
USER_API="http://localhost:${USER_SERVICE_PORT:-8081}"
PRODUCT_API="http://localhost:${PRODUCT_SERVICE_PORT:-8083}"
INVENTORY_API="http://localhost:${INVENTORY_SERVICE_PORT:-8082}"
CART_API="http://localhost:${CART_SERVICE_PORT:-8086}"
ORDER_API="http://localhost:${ORDER_SERVICE_PORT:-8084}"
MAILPIT="http://localhost:${MAILPIT_UI_PORT:-8025}"
# curl is a Windows binary here, so it needs a drive-letter path (C:/...), not /c/...
TMP_DIR="$(cd "$REPO_ROOT" && pwd -W)/logs/e2e/tmp"
mkdir -p "$TMP_DIR"

RUN_ID="$(date +%s)"
USERNAME="e2e_${RUN_ID}"
EMAIL="${USERNAME}@example.test"
PASSWORD="Str0ng!Passw0rd"
SKU="SKU-E2E-${RUN_ID}"
PRICE="129.50"
QUANTITY=4

PASS=0
FAIL=0

ok()   { PASS=$((PASS + 1)); printf '  \033[32mPASS\033[0m %s\n' "$1"; }
bad()  { FAIL=$((FAIL + 1)); printf '  \033[31mFAIL\033[0m %s\n' "$1"; }
info() { printf '\n\033[1m%s\033[0m\n' "$1"; }

# Reads a dotted path out of JSON on stdin without requiring jq.
json_get() {
  node -e '
    let raw = "";
    process.stdin.on("data", (d) => (raw += d));
    process.stdin.on("end", () => {
      try {
        const value = process.argv[1].split(".").reduce((a, k) => (a == null ? a : a[k]), JSON.parse(raw));
        process.stdout.write(value == null ? "" : String(value));
      } catch (e) {
        process.stdout.write("");
      }
    });
  ' "$1"
}

expect_eq() { # description expected actual
  if [ "$2" = "$3" ]; then ok "$1 ($3)"; else bad "$1: expected '$2', got '$3'"; fi
}

# ---------------------------------------------------------------- readiness
info "0. Service readiness"
for target in "user:${USER_API}" "product:${PRODUCT_API}" "inventory:${INVENTORY_API}" \
  "cart:${CART_API}" "order:${ORDER_API}" "payment:http://localhost:${PAYMENT_SERVICE_PORT:-8085}" \
  "notification:http://localhost:${NOTIFICATION_SERVICE_PORT:-8087}"; do
  name=${target%%:*}
  url=${target#*:}
  code=""
  for _ in $(seq 1 20); do
    code=$(curl -s -o /dev/null -w '%{http_code}' "$url/actuator/health" 2>/dev/null)
    [ "$code" = "200" ] && break
    sleep 3
  done
  expect_eq "${name} HEALTH" "200" "$code"
done

# ---------------------------------------------------------------- identity
info "1. Registration and login (user-service)"
register_body="{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\",\"email\":\"${EMAIL}\"}"
code=$(curl -s -o $TMP_DIR/register.json -w '%{http_code}' -X POST "$USER_API/api/v1/users/register" \
  -H 'Content-Type: application/json' -d "$register_body")
case "$code" in 200 | 201) ok "POST /api/v1/users/register ($code)" ;; *) bad "register returned $code: $(cat $TMP_DIR/register.json)" ;; esac

login_body="{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}"
TOKEN=$(curl -s -X POST "$USER_API/api/v1/users/login" -H 'Content-Type: application/json' \
  -d "$login_body" | json_get accessToken)
if [ -n "$TOKEN" ]; then ok "login issued an access token"; else bad "login did not return accessToken"; fi

# Registration always assigns ROLE_USER. The bootstrap administrator promotes the test
# account through the API, which is the only supported provisioning path.
ADMIN_USER="${BOOTSTRAP_ADMIN_USERNAME:-}"
ADMIN_PASS="${BOOTSTRAP_ADMIN_PASSWORD:-}"
if [ -z "$ADMIN_USER" ] || [ -z "$ADMIN_PASS" ]; then
  bad "BOOTSTRAP_ADMIN_USERNAME/BOOTSTRAP_ADMIN_PASSWORD are not set, cannot provision an admin"
  BOOTSTRAP_TOKEN=""
else
  BOOTSTRAP_TOKEN=$(curl -s -X POST "$USER_API/api/v1/users/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" | json_get accessToken)
  if [ -n "$BOOTSTRAP_TOKEN" ]; then ok "bootstrap admin can sign in"; else bad "bootstrap admin login failed"; fi
fi

# A normal user must not be able to grant itself a role.
code=$(curl -s -o $TMP_DIR/escalate.json -w '%{http_code}' -X PATCH "$USER_API/api/v1/users/${USERNAME}/role" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{"role":"ROLE_ADMIN"}')
expect_eq "non-admin cannot change roles" "403" "$code"

# An admin also cannot change its own role (that would be an unrecoverable lockout).
if [ -n "$BOOTSTRAP_TOKEN" ]; then
  code=$(curl -s -o $TMP_DIR/self-demote.json -w '%{http_code}' \
    -X PATCH "$USER_API/api/v1/users/${ADMIN_USER}/role" \
    -H "Authorization: Bearer ${BOOTSTRAP_TOKEN}" -H 'Content-Type: application/json' -d '{"role":"ROLE_USER"}')
  expect_eq "admin cannot change its own role" "400" "$code"
fi

code=$(curl -s -o $TMP_DIR/promote.json -w '%{http_code}' -X PATCH "$USER_API/api/v1/users/${USERNAME}/role" \
  -H "Authorization: Bearer ${BOOTSTRAP_TOKEN:-none}" -H 'Content-Type: application/json' -d '{"role":"ROLE_ADMIN"}')
case "$code" in
  200) ok "admin promoted ${USERNAME} through the API ($code)" ;;
  *) bad "admin promotion returned $code: $(cat $TMP_DIR/promote.json)" ;;
esac

ADMIN_TOKEN=$(curl -s -X POST "$USER_API/api/v1/users/login" -H 'Content-Type: application/json' \
  -d "$login_body" | json_get accessToken)
if [ -n "$ADMIN_TOKEN" ]; then ok "promoted user now receives an admin token"; else bad "could not obtain admin token"; fi

# ---------------------------------------------------------------- catalogue
info "2. Catalogue and stock (product-service, inventory-service)"
product_body="{\"name\":\"E2E Widget\",\"skuCode\":\"${SKU}\",\"price\":${PRICE},\"quantity\":${QUANTITY},\"description\":\"e2e\"}"
code=$(curl -s -o $TMP_DIR/product.json -w '%{http_code}' -X POST "$PRODUCT_API/api/v1/products" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" -H 'Content-Type: application/json' -d "$product_body")
case "$code" in 200 | 201) ok "admin created product ${SKU} ($code)" ;; *) bad "product create returned $code: $(cat $TMP_DIR/product.json)" ;; esac

stock_body="{\"skuCode\":\"${SKU}\",\"name\":\"E2E Widget\",\"quantity\":10}"
code=$(curl -s -o $TMP_DIR/stock.json -w '%{http_code}' -X POST "$INVENTORY_API/api/v1/inventory" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" -H 'Content-Type: application/json' -d "$stock_body")
case "$code" in 200 | 201) ok "admin stocked ${SKU} with 10 units ($code)" ;; *) bad "inventory create returned $code: $(cat $TMP_DIR/stock.json)" ;; esac

# ---------------------------------------------------------------- authz
info "3. Authorization boundaries"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$PRODUCT_API/api/v1/products" \
  -H 'Content-Type: application/json' -d "$product_body")
case "$code" in 401 | 403) ok "anonymous product write rejected ($code)" ;; *) bad "anonymous product write returned $code" ;; esac

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$PRODUCT_API/api/v1/products" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d "{\"name\":\"X\",\"skuCode\":\"SKU-DENY-${RUN_ID}\",\"price\":1,\"quantity\":1}")
case "$code" in 401 | 403) ok "ROLE_USER product write rejected ($code)" ;; *) bad "ROLE_USER product write returned $code" ;; esac

code=$(curl -s -o /dev/null -w '%{http_code}' "$CART_API/api/v1/cart/someone-else")
case "$code" in 401 | 403) ok "cart read of a foreign user rejected ($code)" ;; *) bad "foreign cart read returned $code" ;; esac

# ---------------------------------------------------------------- cart
info "4. Cart (cart-service, Redis-backed)"
code=$(curl -s -o $TMP_DIR/cart-add.json -w '%{http_code}' -X POST "$CART_API/api/v1/cart/${USERNAME}/add" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d "{\"productId\":\"${SKU}\",\"quantity\":2}")
case "$code" in 200 | 201) ok "added 2 x ${SKU} to the cart ($code)" ;; *) bad "cart add returned $code: $(cat $TMP_DIR/cart-add.json)" ;; esac

cart_qty=$(curl -s "$CART_API/api/v1/cart/${USERNAME}" -H "Authorization: Bearer ${TOKEN}" | json_get data.0.quantity)
expect_eq "cart contains 2 units" "2" "$cart_qty"

# ---------------------------------------------------------------- saga
info "5. Order saga (order -> inventory -> payment -> order)"
idem="idem-${RUN_ID}"
order_body="{\"orderItems\":[{\"skuCode\":\"${SKU}\",\"quantity\":2}],\"idempotencyKey\":\"${idem}\"}"
curl -s -o $TMP_DIR/order.json -X POST "$ORDER_API/api/v1/orders" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d "$order_body"
ORDER_NUMBER=$(json_get orderNumber < $TMP_DIR/order.json)
if [ -n "$ORDER_NUMBER" ]; then ok "order ${ORDER_NUMBER} created"; else bad "order creation failed: $(cat $TMP_DIR/order.json)"; fi

# The same idempotency key must not create a second order.
second=$(curl -s -X POST "$ORDER_API/api/v1/orders" -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' -d "$order_body" | json_get orderNumber)
expect_eq "replayed idempotency key returns the same order" "$ORDER_NUMBER" "$second"

STATUS=""
for _ in $(seq 1 30); do
  STATUS=$(curl -s "$ORDER_API/api/v1/orders/${ORDER_NUMBER}" -H "Authorization: Bearer ${TOKEN}" | json_get status)
  [ "$STATUS" = "PAID" ] && break
  [ "$STATUS" = "CANCELLED" ] && break
  sleep 2
done
expect_eq "order reached PAID through payment-authorized" "PAID" "$STATUS"

stock_left=$(curl -s "$INVENTORY_API/api/v1/inventory/${SKU}" -H "Authorization: Bearer ${ADMIN_TOKEN}" | json_get quantity)
expect_eq "inventory deducted 2 units (10 -> 8)" "8" "$stock_left"

cart_after=$(curl -s "$CART_API/api/v1/cart/${USERNAME}" -H "Authorization: Bearer ${TOKEN}" | json_get data.0.productId)
expect_eq "cart cleared by order-confirmed" "" "$cart_after"

# ---------------------------------------------------------------- notification
info "6. Notification delivery (notification-service -> Mailpit)"
found=""
for _ in $(seq 1 20); do
  found=$(curl -s "$MAILPIT/api/v1/messages?limit=50" | grep -c "$EMAIL")
  [ "$found" -gt 0 ] && break
  sleep 3
done
if [ "$found" -gt 0 ]; then ok "Mailpit received at least one email for ${EMAIL}"; else bad "no email reached Mailpit for ${EMAIL}"; fi

# ---------------------------------------------------------------- gateway
info "7. Gateway (Kong :${KONG_PROXY_PORT:-8000})"
code=$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/api/v1/products?page=0&size=5")
expect_eq "GET /api/v1/products through Kong" "200" "$code"

code=$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/api/v1/orders/${ORDER_NUMBER}" \
  -H "Authorization: Bearer ${TOKEN}")
expect_eq "GET order through Kong with token" "200" "$code"

code=$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/api/v1/orders/${ORDER_NUMBER}")
case "$code" in 401 | 403) ok "gateway forwards unauthenticated order read as $code" ;; *) bad "unauthenticated order read via Kong returned $code" ;; esac

corr=$(curl -s -D - -o /dev/null "$GATEWAY/api/v1/products?page=0&size=1" | grep -i '^x-correlation-id:' | tr -d '\r' | cut -d' ' -f2)
if [ -n "$corr" ]; then ok "gateway echoes X-Correlation-Id (${corr:0:8}...)"; else bad "no X-Correlation-Id from the gateway"; fi

acao=$(curl -s -D - -o /dev/null -X OPTIONS "$GATEWAY/api/v1/products" \
  -H 'Origin: http://localhost:3000' -H 'Access-Control-Request-Method: GET' |
  grep -i '^access-control-allow-origin:' | tr -d '\r' | cut -d' ' -f2)
expect_eq "CORS allows the configured origin" "http://localhost:3000" "$acao"

# Kong always echoes the configured origin rather than the caller's, so the property
# that matters is that an unlisted origin is never reflected back.
acao_bad=$(curl -s -D - -o /dev/null -X OPTIONS "$GATEWAY/api/v1/products" \
  -H 'Origin: http://evil.example' -H 'Access-Control-Request-Method: GET' |
  grep -i '^access-control-allow-origin:' | tr -d '\r' | cut -d' ' -f2)
if [ "$acao_bad" != "http://evil.example" ]; then
  ok "CORS never reflects an unlisted origin (answered '${acao_bad:-none}')"
else
  bad "CORS reflected the unlisted origin"
fi

# ---------------------------------------------------------------- payment failure path
info "8. Compensation path (declined payment)"
DECLINE_SKU="SKU-DECLINE-${RUN_ID}"
curl -s -o /dev/null -X POST "$PRODUCT_API/api/v1/products" -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"Decline Widget\",\"skuCode\":\"${DECLINE_SKU}\",\"price\":42.99,\"quantity\":1}"
curl -s -o /dev/null -X POST "$INVENTORY_API/api/v1/inventory" -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"skuCode\":\"${DECLINE_SKU}\",\"name\":\"Decline Widget\",\"quantity\":5}"

curl -s -o $TMP_DIR/decline.json -X POST "$ORDER_API/api/v1/orders" -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"orderItems\":[{\"skuCode\":\"${DECLINE_SKU}\",\"quantity\":1}],\"idempotencyKey\":\"idem-d-${RUN_ID}\"}"
DECLINE_ORDER=$(json_get orderNumber < $TMP_DIR/decline.json)

STATUS=""
for _ in $(seq 1 30); do
  STATUS=$(curl -s "$ORDER_API/api/v1/orders/${DECLINE_ORDER}" -H "Authorization: Bearer ${TOKEN}" | json_get status)
  [ "$STATUS" = "CANCELLED" ] && break
  [ "$STATUS" = "PAID" ] && break
  sleep 2
done
expect_eq "order with a .99 total is CANCELLED by payment-failed" "CANCELLED" "$STATUS"

# The order flips to CANCELLED before inventory finishes releasing, so poll for the restock.
restocked=""
for _ in $(seq 1 15); do
  restocked=$(curl -s "$INVENTORY_API/api/v1/inventory/${DECLINE_SKU}" -H "Authorization: Bearer ${ADMIN_TOKEN}" | json_get quantity)
  [ "$restocked" = "5" ] && break
  sleep 2
done
expect_eq "cancelled order released its reservation (5 units back)" "5" "$restocked"

# ---------------------------------------------------------------- summary
info "Summary"
printf '  passes: %d   failures: %d\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ] && { printf '\n\033[32mE2E PASSED\033[0m\n'; exit 0; }
printf '\n\033[31mE2E FAILED\033[0m\n'
exit 1
