#!/usr/bin/env bash
# STK-RFC-007 / EG-055 — mkcert CA oracle (no PLAYWRIGHT_TLS_INSECURE, no curl -k).
set -euo pipefail

CADDY_HTTPS_PORT="${CADDY_HTTPS_PORT:-8443}"
mkcert_bin="mkcert"
if [[ -x "${HOME}/.local/bin/mkcert" ]]; then
  mkcert_bin="${HOME}/.local/bin/mkcert"
fi
if ! command -v "$mkcert_bin" >/dev/null 2>&1 && [[ ! -x "$mkcert_bin" ]]; then
  echo "mkcert is required for the cert oracle" >&2
  exit 1
fi
ca="$("$mkcert_bin" -CAROOT)/rootCA.pem"
if [[ ! -f "$ca" ]]; then
  echo "mkcert CA not found at $ca" >&2
  exit 1
fi

code=$(curl -sS --cacert "$ca" \
  --resolve "app.payment-quality.local:${CADDY_HTTPS_PORT}:127.0.0.1" \
  -o /dev/null -w '%{http_code}' \
  "https://app.payment-quality.local:${CADDY_HTTPS_PORT}")
if [[ ! "$code" =~ ^(200|301|302|303|307|308)$ ]]; then
  echo "Expected 2xx/3xx from app vhost with mkcert CA, got $code" >&2
  exit 1
fi
echo "STK-RFC-007 OK (HTTP $code) using $ca"
