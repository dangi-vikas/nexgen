#!/usr/bin/env bash
# Gateway-wide plugins. Runs before the services so every route inherits them.
set -euo pipefail
# shellcheck source=../utils/load-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../utils/load-env.sh"
# shellcheck source=../utils/kong.sh
. "$KONG_DIR/utils/kong.sh"

echo "Configuring global plugins on ${KONG_ADMIN_URL}"

# Tagged so delete-all.sh can find and remove them without jq.
# Metrics are scraped from the admin API on :8001/metrics.
ensure_plugin "" prometheus '{"name":"prometheus","tags":["nexgen-gateway"]}'

# One correlation id per request, matching CorrelationIdFilter in every service.
ensure_plugin "" correlation-id '{"name":"correlation-id","tags":["nexgen-gateway"],"config":{
  "header_name":"X-Correlation-Id",
  "generator":"uuid",
  "echo_downstream":true}}'

# Deliberately no `jwt` plugin: user/product/inventory/cart/order are OAuth2 resource
# servers and validate the access token themselves. A gateway JWT plugin needs Kong
# consumers whose credentials would have to be mirrored from user-service, and the
# previous scripts created consumers without ever issuing matching credentials, so
# every proxied request failed with 401.
