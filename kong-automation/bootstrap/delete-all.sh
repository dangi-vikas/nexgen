#!/usr/bin/env bash
# Removes the gateway objects created by setup-all.sh. Safe to run when nothing exists.
# No jq dependency: global plugins are tagged "nexgen-gateway" and their ids are
# extracted from the compact Admin API JSON.
set -euo pipefail
# shellcheck source=../utils/load-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../utils/load-env.sh"
# shellcheck source=../utils/kong.sh
. "$KONG_DIR/utils/kong.sh"

echo "Removing gateway configuration from ${KONG_ADMIN_URL}"

for name in "$USER_SERVICE_NAME" "$PRODUCT_SERVICE_NAME" "$CART_SERVICE_NAME" \
  "$ORDER_SERVICE_NAME" "$INVENTORY_SERVICE_NAME"; do
  kong_delete "/services/${name}"
done

for id in $(kong_get "/plugins?tags=nexgen-gateway" | grep -o '"id":"[^"]*"' | cut -d'"' -f4); do
  kong_delete "/plugins/${id}"
done

echo "Gateway configuration removed."
