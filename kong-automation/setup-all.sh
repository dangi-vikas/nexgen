#!/usr/bin/env bash
# Registers every public service with Kong. Idempotent: run it after each `up`.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
# shellcheck source=utils/load-env.sh
. ./utils/load-env.sh

echo "Kong admin API : ${KONG_ADMIN_URL}"
echo "Service upstream: ${KONG_SERVICE_HOST} (services must be running on the host)"

./bootstrap/setup-kong-plugins.sh

for service in user product cart order inventory; do
  ./services/setup-${service}-service.sh
done

echo
echo "Gateway ready on http://localhost:${KONG_PROXY_PORT:-8000}"
echo "  GET  /api/v1/products"
echo "  POST /api/v1/users/register"
