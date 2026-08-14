#!/usr/bin/env bash
# --full OIDC split: loopback discovery (HTTPS issuer, HTTP token) vs Caddy 404.
# Requires scripts/dev-stack.sh --full and:
#   PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
set -euo pipefail

CADDY_HTTPS_PORT="${CADDY_HTTPS_PORT:-8443}"
FRONTEND_CONTAINER="${FRONTEND_CONTAINER:-payment-quality-frontend}"
PASSWORD="${PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD:?PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD is required}"
USERNAME="${PLAYWRIGHT_MERCHANT_MANAGER_USERNAME:-merchant.manager}"

runtime_bin=""
if command -v docker >/dev/null 2>&1; then
  runtime_bin=docker
elif command -v podman >/dev/null 2>&1; then
  runtime_bin=podman
else
  echo "docker or podman is required" >&2
  exit 1
fi

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
CURL_TLS+=(--resolve "app.payment-quality.local:${CADDY_HTTPS_PORT}:127.0.0.1")

discovery_json="$("$runtime_bin" exec "$FRONTEND_CONTAINER" node -e "
fetch('http://127.0.0.1:3000/__oidc/openid-configuration').then(async (r) => {
  if (!r.ok) {
    console.error('loopback discovery HTTP', r.status)
    process.exit(2)
  }
  const j = await r.json()
  process.stdout.write(JSON.stringify(j))
}).catch((err) => {
  console.error(err)
  process.exit(2)
})
")"

issuer="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["issuer"])' <<<"$discovery_json")"
token_endpoint="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token_endpoint"])' <<<"$discovery_json")"

if [[ "$issuer" != https://auth.payment-quality.local* ]]; then
  echo "issuer must be public HTTPS, got $issuer" >&2
  exit 3
fi
if [[ "$token_endpoint" != http://payment-quality-keycloak:8080* ]]; then
  echo "token_endpoint must be Keycloak HTTP inside compose, got $token_endpoint" >&2
  exit 4
fi

caddy_code="$("${CURL_TLS[@]}" -o /dev/null -w '%{http_code}' \
  "https://app.payment-quality.local:${CADDY_HTTPS_PORT}/__oidc/openid-configuration")"
if [[ "$caddy_code" != 404 ]]; then
  echo "Caddy /__oidc/openid-configuration must be 404, got $caddy_code" >&2
  exit 5
fi

grant_ok="$("$runtime_bin" exec -e USERNAME="$USERNAME" -e PASSWORD="$PASSWORD" \
  -e TOKEN_ENDPOINT="$token_endpoint" "$FRONTEND_CONTAINER" node -e "
const body = new URLSearchParams({
  client_id: 'payment-quality-dashboard',
  grant_type: 'password',
  username: process.env.USERNAME,
  password: process.env.PASSWORD,
})
fetch(process.env.TOKEN_ENDPOINT, {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body,
}).then(async (r) => {
  const j = await r.json()
  if (!r.ok || !j.access_token) {
    console.error('password grant failed', r.status, j)
    process.exit(6)
  }
  process.stdout.write('ok')
})
")"
if [[ "$grant_ok" != ok ]]; then
  echo "password grant on internal token_endpoint failed" >&2
  exit 6
fi

echo "OIDC back-channel oracle OK (issuer HTTPS, token HTTP, Caddy /__oidc 404, password grant)"
