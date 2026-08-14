#!/usr/bin/env bash
# RA-RFC-031 live + RA-RFC-034: POST/GET through Caddy vs Spring :8080.
# Requires scripts/dev-stack.sh --tls (or --full) and:
#   PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
# On --full, rootless pasta often does not expose container :8080 on host
# loopback; then compare Caddy with docker/podman exec into the backend
# (curl http://127.0.0.1:8080 inside the container).
set -euo pipefail

CADDY_HTTPS_PORT="${CADDY_HTTPS_PORT:-8443}"
MERCHANT_ID="${TLS_LAB_MERCHANT_ID:-00000000-0000-0000-0000-0000000000b1}"
PASSWORD="${PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD:?PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD is required}"
USERNAME="${PLAYWRIGHT_MERCHANT_MANAGER_USERNAME:-merchant.manager}"
KEYCLOAK_TOKEN_URL="${KEYCLOAK_TOKEN_URL:-http://localhost:8081/realms/payment-quality/protocol/openid-connect/token}"
HTTP_API="${HTTP_API:-http://localhost:8080}"
TLS_API="https://api.payment-quality.local:${CADDY_HTTPS_PORT}"
BACKEND_CONTAINER="${BACKEND_CONTAINER:-payment-quality-backend}"

mkcert_ca="${NODE_EXTRA_CA_CERTS:-}"
if [[ -z "$mkcert_ca" && -x "${HOME}/.local/bin/mkcert" ]]; then
  mkcert_ca="$("${HOME}/.local/bin/mkcert" -CAROOT 2>/dev/null)/rootCA.pem"
elif [[ -z "$mkcert_ca" ]] && command -v mkcert >/dev/null 2>&1; then
  mkcert_ca="$(mkcert -CAROOT 2>/dev/null)/rootCA.pem"
fi

CURL_TLS=(curl -sS)
if [[ -n "$mkcert_ca" && -f "$mkcert_ca" ]]; then
  CURL_TLS+=(--cacert "$mkcert_ca")
else
  CURL_TLS+=(-k)
fi
CURL_TLS+=(--resolve "api.payment-quality.local:${CADDY_HTTPS_PORT}:127.0.0.1")

runtime_bin=""
if command -v docker >/dev/null 2>&1; then
  runtime_bin=docker
elif command -v podman >/dev/null 2>&1; then
  runtime_bin=podman
fi

http_via_exec=0
COMPARE_HTTP=0
if curl -sf --max-time 2 "${HTTP_API}/api/status" >/dev/null; then
  COMPARE_HTTP=1
elif [[ -n "$runtime_bin" ]] && "$runtime_bin" exec "$BACKEND_CONTAINER" curl -sf --max-time 2 http://127.0.0.1:8080/api/status >/dev/null 2>&1; then
  COMPARE_HTTP=1
  http_via_exec=1
  echo "Note: ${HTTP_API} unreachable on the host; dual oracle uses ${runtime_bin} exec ${BACKEND_CONTAINER} curl http://127.0.0.1:8080" >&2
else
  echo "Note: ${HTTP_API} unreachable and container exec failed; TLS-only Caddy oracle" >&2
fi

token="$(curl -sS -X POST "$KEYCLOAK_TOKEN_URL" \
  -d "client_id=payment-quality-dashboard" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}" \
  -d "grant_type=password" | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')"

ref="TLS-LOC-$(date +%s)"
idem="idem-${ref}"
body=$(printf '{"amountMinor":12500,"currency":"PLN","clientOrderReference":"%s"}' "$ref")

post_headers=(-H "Authorization: Bearer ${token}" -H "Content-Type: application/json" -H "Idempotency-Key: ${idem}" -D - -o /tmp/tls-lab-location-body.json)

tls_headers=$("${CURL_TLS[@]}" "${post_headers[@]}" -X POST "${TLS_API}/api/merchants/${MERCHANT_ID}/payment-orders" -d "$body")
tls_loc=$(printf '%s' "$tls_headers" | awk 'tolower($1)=="location:" {print $2}' | tr -d '\r')

assert_relative() {
  local label="$1" loc="$2"
  if [[ "$loc" != /api/merchants/*/payment-orders/* ]]; then
    echo "${label} Location is not a relative payment-order URI: ${loc}" >&2
    exit 1
  fi
  if [[ "$loc" == http* ]]; then
    echo "${label} Location must not be absolute: ${loc}" >&2
    exit 1
  fi
}

assert_relative "TLS" "$tls_loc"
order_id="${tls_loc##*/}"
tls_get=$("${CURL_TLS[@]}" -D - -o /dev/null -H "Authorization: Bearer ${token}" "${TLS_API}/api/merchants/${MERCHANT_ID}/payment-orders/${order_id}")
tls_vary=$(printf '%s' "$tls_get" | awk 'tolower($1)=="vary:" {print tolower($0)}' | tr -d '\r')
tls_cc=$(printf '%s' "$tls_get" | awk 'tolower($1)=="cache-control:" {print tolower($0)}' | tr -d '\r')

if [[ "$tls_vary" != *"authorization"* ]]; then
  echo "TLS GET missing Vary: Authorization" >&2
  exit 1
fi
if [[ -z "$tls_cc" ]]; then
  echo "Cache-Control missing on TLS GET" >&2
  exit 1
fi

if [[ "$COMPARE_HTTP" == "1" ]]; then
  if [[ "$http_via_exec" == "1" ]]; then
    http_headers=$("$runtime_bin" exec "$BACKEND_CONTAINER" curl -sS -D - -o /dev/null \
      -H "Authorization: Bearer ${token}" -H "Content-Type: application/json" -H "Idempotency-Key: ${idem}-http" \
      -X POST "http://127.0.0.1:8080/api/merchants/${MERCHANT_ID}/payment-orders" -d "$body")
  else
    http_headers=$(curl -sS "${post_headers[@]}" -X POST "${HTTP_API}/api/merchants/${MERCHANT_ID}/payment-orders" -d "$body")
  fi
  http_loc=$(printf '%s' "$http_headers" | awk 'tolower($1)=="location:" {print $2}' | tr -d '\r')
  assert_relative "HTTP" "$http_loc"
  http_order_id="${http_loc##*/}"
  if [[ "$http_via_exec" == "1" ]]; then
    http_get=$("$runtime_bin" exec "$BACKEND_CONTAINER" curl -sS -D - -o /dev/null \
      -H "Authorization: Bearer ${token}" \
      "http://127.0.0.1:8080/api/merchants/${MERCHANT_ID}/payment-orders/${http_order_id}")
  else
    http_get=$(curl -sS -D - -o /dev/null -H "Authorization: Bearer ${token}" "${HTTP_API}/api/merchants/${MERCHANT_ID}/payment-orders/${order_id}")
  fi
  http_vary=$(printf '%s' "$http_get" | awk 'tolower($1)=="vary:" {print tolower($0)}' | tr -d '\r')
  http_cc=$(printf '%s' "$http_get" | awk 'tolower($1)=="cache-control:" {print tolower($0)}' | tr -d '\r')
  if [[ "$http_vary" != *"authorization"* ]]; then
    echo "HTTP GET missing Vary: Authorization" >&2
    exit 1
  fi
  if [[ -z "$http_cc" ]]; then
    echo "Cache-Control missing on HTTP GET" >&2
    exit 1
  fi
  echo "RA-RFC-031/034 OK (Caddy vs ${HTTP_API}${http_via_exec:+ via ${runtime_bin} exec})"
  echo "  HTTP Location: $http_loc"
  echo "  TLS  Location: $tls_loc"
  echo "  HTTP $http_vary | $http_cc"
  echo "  TLS  $tls_vary | $tls_cc"
else
  echo "RA-RFC-031/034 OK (Caddy only)"
  echo "  TLS  Location: $tls_loc"
  echo "  TLS  $tls_vary | $tls_cc"
fi
