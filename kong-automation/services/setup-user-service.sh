#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=../utils/load-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../utils/load-env.sh"
# shellcheck source=../utils/kong.sh
. "$KONG_DIR/utils/kong.sh"

setup_service "$USER_SERVICE_NAME" "$USER_SERVICE_URL" "$USER_SERVICE_ROUTE" "$USER_RATE_LIMIT"
