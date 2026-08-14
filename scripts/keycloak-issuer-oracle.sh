#!/usr/bin/env bash
# Fail unless Keycloak OIDC discovery advertises the expected issuer prefix.
# Usage: scripts/keycloak-issuer-oracle.sh <issuer-prefix> [discovery-url]
set -euo pipefail

EXPECTED_PREFIX="${1:?usage: keycloak-issuer-oracle.sh <issuer-prefix> [discovery-url]}"
DISCOVERY_URL="${2:-http://127.0.0.1:8081/realms/payment-quality/.well-known/openid-configuration}"

body="$(curl -sf --max-time 10 "$DISCOVERY_URL")" || {
  echo "Keycloak discovery failed: $DISCOVERY_URL" >&2
  exit 1
}

issuer="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["issuer"])' <<<"$body")"
case "$issuer" in
  "${EXPECTED_PREFIX}"*)
    echo "Keycloak issuer OK: $issuer"
    ;;
  *)
    echo "Keycloak issuer mismatch." >&2
    echo "  discovery $DISCOVERY_URL" >&2
    echo "  got       $issuer" >&2
    echo "  expected  prefix $EXPECTED_PREFIX" >&2
    echo "Recreate Keycloak after switching HTTP ↔ HTTPS compose (sticky KC_HOSTNAME)." >&2
    exit 1
    ;;
esac
