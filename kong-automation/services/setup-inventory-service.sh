#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=../utils/load-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../utils/load-env.sh"
# shellcheck source=../utils/kong.sh
. "$KONG_DIR/utils/kong.sh"

setup_service "$INVENTORY_SERVICE_NAME" "$INVENTORY_SERVICE_URL" "$INVENTORY_SERVICE_ROUTE" "$INVENTORY_RATE_LIMIT"
