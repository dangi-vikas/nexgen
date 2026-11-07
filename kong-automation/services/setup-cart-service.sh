#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=../utils/load-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../utils/load-env.sh"
# shellcheck source=../utils/kong.sh
. "$KONG_DIR/utils/kong.sh"

setup_service "$CART_SERVICE_NAME" "$CART_SERVICE_URL" "$CART_SERVICE_ROUTE" "$CART_RATE_LIMIT"
