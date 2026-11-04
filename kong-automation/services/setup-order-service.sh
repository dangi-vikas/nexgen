#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=../utils/load-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../utils/load-env.sh"
# shellcheck source=../utils/kong.sh
. "$KONG_DIR/utils/kong.sh"

setup_service "$ORDER_SERVICE_NAME" "$ORDER_SERVICE_URL" "$ORDER_SERVICE_ROUTE" "$ORDER_RATE_LIMIT"
