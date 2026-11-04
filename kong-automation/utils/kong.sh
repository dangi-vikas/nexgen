#!/usr/bin/env bash
# Idempotent Kong Admin API helpers.
#
# The previous scripts used POST everywhere, so a second run failed with 409
# "already exists" and the gateway was left half-configured. Everything here is a PUT
# keyed by name, which creates or replaces in one step.
set -euo pipefail

: "${KONG_ADMIN_URL:?source utils/load-env.sh before utils/kong.sh}"

_kong() { # method path [curl args...]
  local method=$1 path=$2
  shift 2
  local response
  if ! response=$(curl -sS -X "$method" "${KONG_ADMIN_URL}${path}" "$@"); then
    echo "  ! ${method} ${path}: cannot reach Kong at ${KONG_ADMIN_URL}" >&2
    return 1
  fi
  if printf '%s' "$response" | grep -q '"message"'; then
    echo "  ! ${method} ${path}: ${response}" >&2
    return 1
  fi
}

kong_get() {
  curl -fsS "${KONG_ADMIN_URL}$1"
}

kong_put() { # path json
  _kong PUT "$1" -H 'Content-Type: application/json' -d "$2"
}

kong_delete() { # path
  local code
  code=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "${KONG_ADMIN_URL}$1")
  case "$code" in
    200 | 204) echo "  - deleted $1" ;;
    404) echo "  - $1 already absent" ;;
    *)
      echo "  ! DELETE $1 returned $code" >&2
      return 1
      ;;
  esac
}

ensure_plugin() { # scope name json  (scope "" = global)
  local scope=$1 name=$2 json=$3
  if kong_get "${scope}/plugins" | grep -q "\"name\":\"${name}\""; then
    echo "  = plugin ${name} on ${scope:-global} (already present)"
    return 0
  fi
  kong_put "${scope}/plugins" "$json"
  echo "  + plugin ${name} on ${scope:-global}"
}

# Ensures the service, its route and the two per-service plugins exist.
setup_service() { # name url route rate_limit_per_minute
  local name=$1 url=$2 route=$3 rate=$4

  echo "Setting up ${name}"
  kong_put "/services/${name}" "{\"name\":\"${name}\",\"url\":\"${url}\"}"
  kong_put "/services/${name}/routes/${name}-route" \
    "{\"name\":\"${name}-route\",\"paths\":[\"${route}\"],\"strip_path\":false}"

  ensure_plugin "/services/${name}" cors "{\"name\":\"cors\",\"config\":{
    \"origins\":[\"${WEB_ORIGIN}\"],
    \"credentials\":true,
    \"methods\":[\"GET\",\"POST\",\"PUT\",\"PATCH\",\"DELETE\",\"OPTIONS\"],
    \"headers\":[\"Authorization\",\"Content-Type\",\"X-Correlation-Id\",\"Idempotency-Key\"],
    \"exposed_headers\":[\"X-Correlation-Id\"],
    \"max_age\":3600}}"

  ensure_plugin "/services/${name}" rate-limiting "{\"name\":\"rate-limiting\",\"config\":{
    \"minute\":${rate},
    \"policy\":\"local\",
    \"fault_tolerant\":true}}"

  echo "  -> http://localhost:${KONG_PROXY_PORT:-8000}${route}"
}
