#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=../utils/load-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../utils/load-env.sh"
# shellcheck source=../utils/kong.sh
. "$KONG_DIR/utils/kong.sh"

setup_service "$PRODUCT_SERVICE_NAME" "$PRODUCT_SERVICE_URL" "$PRODUCT_SERVICE_ROUTE" "$PRODUCT_RATE_LIMIT"
