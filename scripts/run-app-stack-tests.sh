#!/usr/bin/env bash
# Run live Playwright (POM E2E + REST) and optional backend tests against HTTP
# compose `--app`. Caddy / HTTPS is not required.
#
#   scripts/dev-stack.sh --app
#   export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=...
#   export PLAYWRIGHT_TENANT_ADMIN_PASSWORD=...
#   export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=...
#   scripts/run-app-stack-tests.sh            # Playwright only
#   scripts/run-app-stack-tests.sh --backend  # also Surefire (skips restkit/)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND="$REPO_ROOT/apps/frontend"
BACKEND="$REPO_ROOT/apps/backend"
# --app compose sets NUXT_OAUTH_OIDC_REDIRECT_URL to 127.0.0.1 (not localhost).
# Host DX Playwright stays on localhost; this script is --app only.
BASE_URL="${PLAYWRIGHT_BASE_URL:-http://127.0.0.1:3000}"
RUN_BACKEND=0
RUN_VISUAL=0
RUN_RLS_OFF=0
RUN_RLS_SPRING_OFF=0
RUN_MIRROR_OFF=0

usage() {
  cat <<'EOF'
Usage: scripts/run-app-stack-tests.sh [--backend] [--visual] [--rls-off] [--rls-spring-off] [--mirror-off]

Requires a healthy HTTP stack from `scripts/dev-stack.sh --app` (no Caddy).
Default `pnpm test:e2e` is the live POM suite (`tests-pom` only).

  --visual   Visual Lab screenshots + ARIA snapshots (PLAYWRIGHT_VISUAL=1)
  --rls-off         FE RLS flag off on :3010
  --rls-spring-off  Spring RLS off on :8082 + Nuxt :3011
  --mirror-off      FE Mirror/Session lab flag off on :3012
  --backend         Surefire without restkit/

Passwords only from the environment:
  PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD
  PLAYWRIGHT_TENANT_ADMIN_PASSWORD
  PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD
  PLAYWRIGHT_SUPPORT_AGENT_PASSWORD
  PLAYWRIGHT_READ_ONLY_PASSWORD
  PLAYWRIGHT_MERCHANT_DENIED_PASSWORD
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --backend) RUN_BACKEND=1 ;;
    --visual) RUN_VISUAL=1 ;;
    --rls-off) RUN_RLS_OFF=1 ;;
    --rls-spring-off) RUN_RLS_SPRING_OFF=1 ;;
    --mirror-off) RUN_MIRROR_OFF=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "$name is required" >&2
    exit 2
  fi
}

require_env PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD
require_env PLAYWRIGHT_TENANT_ADMIN_PASSWORD
require_env PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD
require_env PLAYWRIGHT_SUPPORT_AGENT_PASSWORD
require_env PLAYWRIGHT_READ_ONLY_PASSWORD
require_env PLAYWRIGHT_MERCHANT_DENIED_PASSWORD
for worker_index in 0 1 2 3; do
  worker_var="PLAYWRIGHT_MERCHANT_MANAGER_W${worker_index}_PASSWORD"
  if [[ -z "${!worker_var:-}" ]]; then
    export "${worker_var}=merchant.manager.w${worker_index}"
  fi
done

echo "== oracles =="
issuer="$(curl -fsS http://127.0.0.1:8081/realms/payment-quality/.well-known/openid-configuration \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["issuer"])')"
if [[ "$issuer" != "http://localhost:8081/realms/payment-quality" ]]; then
  echo "Keycloak issuer is $issuer (want http://localhost:8081). Use --app, not --full/--tls." >&2
  exit 1
fi
spring_code="$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/status)"
nuxt_code="$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL")"
echo "issuer=$issuer spring=$spring_code nuxt=$nuxt_code"
if [[ "$nuxt_code" == "500" ]]; then
  echo "Nuxt returned 500 at $BASE_URL (compose frontend on --app, not host .nuxt/dev)." >&2
  echo "Fix: scripts/dev-stack.sh --stop  (if host pnpm dev holds :3000)" >&2
  echo "Then: scripts/dev-stack.sh --down && scripts/dev-stack.sh --app" >&2
  exit 1
fi
if [[ "$spring_code" != "200" || ! "$nuxt_code" =~ ^(200|302)$ ]]; then
  echo "Stack is not healthy (spring=$spring_code nuxt=$nuxt_code). Start with: scripts/dev-stack.sh --app" >&2
  echo "Host nuxt.dev on :3000 collides with --app — run scripts/dev-stack.sh --stop first." >&2
  exit 1
fi

export PLAYWRIGHT_SKIP_WEBSERVER=1
export PLAYWRIGHT_BASE_URL="$BASE_URL"
export PLAYWRIGHT_LIVE_RUN_ID="${PLAYWRIGHT_LIVE_RUN_ID:-APPSTACK01}"

echo "== Playwright POM (live E2E + BFF REST; only tests-pom) =="
(cd "$FRONTEND" && corepack pnpm exec playwright test --config playwright.pom.config.ts)

if [[ "$RUN_VISUAL" -eq 1 ]]; then
  echo "== Playwright visual / ARIA (live --app) =="
  (cd "$FRONTEND" && PLAYWRIGHT_VISUAL=1 corepack pnpm exec playwright test --config playwright.pom.config.ts --project=chromium-visual --project=setup-platform-admin --project=setup-merchant-manager)
fi

if [[ "$RUN_RLS_OFF" -eq 1 ]]; then
  echo "== compose frontend RLS flag-off :3010 =="
  docker compose --env-file "$REPO_ROOT/infra/compose/.env" \
    -f "$REPO_ROOT/infra/compose/compose.yml" \
    -f "$REPO_ROOT/infra/compose/compose.app.http.yml" \
    -f "$REPO_ROOT/infra/compose/compose.app.rls-flag-off.yml" \
    up -d payment-quality-frontend-rls-off
  docker stop payment-quality-frontend-rls-off >/dev/null
  docker start payment-quality-frontend-rls-off >/dev/null
  for _ in $(seq 1 30); do
    if curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:3010 | grep -qE '302|200'; then
      break
    fi
    sleep 1
  done
  echo "== Playwright RLS flag-off POM =="
  (cd "$FRONTEND" && corepack pnpm exec playwright test --config playwright.rls-flag-off.config.ts)
fi

if [[ "$RUN_RLS_SPRING_OFF" -eq 1 ]]; then
  echo "== compose Spring RLS-off :8082 + Nuxt :3011 =="
  docker compose --env-file "$REPO_ROOT/infra/compose/.env" \
    -f "$REPO_ROOT/infra/compose/compose.yml" \
    -f "$REPO_ROOT/infra/compose/compose.app.http.yml" \
    -f "$REPO_ROOT/infra/compose/compose.app.rls-spring-off.yml" \
    up -d payment-quality-backend-rls-off payment-quality-frontend-rls-spring-off
  docker stop payment-quality-backend-rls-off payment-quality-frontend-rls-spring-off >/dev/null
  docker start payment-quality-backend-rls-off
  docker start payment-quality-frontend-rls-spring-off
  for _ in $(seq 1 40); do
    if curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:3011 | grep -qE '302|200'; then
      break
    fi
    sleep 2
  done
  echo "== Playwright RLS Spring-off POM =="
  (cd "$FRONTEND" && corepack pnpm exec playwright test --config playwright.pom.rls-spring-off.config.ts)
fi

if [[ "$RUN_MIRROR_OFF" -eq 1 ]]; then
  echo "== compose frontend Mirror flag-off :3012 =="
  docker compose --env-file "$REPO_ROOT/infra/compose/.env" \
    -f "$REPO_ROOT/infra/compose/compose.yml" \
    -f "$REPO_ROOT/infra/compose/compose.app.http.yml" \
    -f "$REPO_ROOT/infra/compose/compose.app.mirror-flag-off.yml" \
    up -d payment-quality-frontend-mirror-off
  docker stop payment-quality-frontend-mirror-off >/dev/null
  docker start payment-quality-frontend-mirror-off >/dev/null
  for _ in $(seq 1 30); do
    if curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:3012 | grep -qE '302|200'; then
      break
    fi
    sleep 1
  done
  echo "== Playwright Mirror flag-off POM =="
  (cd "$FRONTEND" && corepack pnpm exec playwright test --config playwright.mirror-flag-off.config.ts)
fi

if [[ "$RUN_BACKEND" -eq 1 ]]; then
  echo "== backend Surefire (restkit excluded; Ryuk off) =="
  export TESTCONTAINERS_RYUK_DISABLED=true
  (cd "$BACKEND" && ./mvnw test -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**')
fi

echo "OK: live stack tests finished"
