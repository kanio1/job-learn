#!/usr/bin/env bash
# Raise the local HTTP (or TLS overlay) lab stack:
#   Postgres + Keycloak (compose) → Spring Boot `dev` → Nuxt.
# Backend and frontend stay on the host for hot reload.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_ENV="$REPO_ROOT/infra/compose/.env"
COMPOSE_FILE="$REPO_ROOT/infra/compose/compose.yml"
COMPOSE_TLS_FILE="$REPO_ROOT/infra/compose/compose.tls.yml"
LOG_DIR="$REPO_ROOT/tmp/dev-stack"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"

TLS=0
STOP=0
DOWN=0
CADDY_HTTPS_PORT="${CADDY_HTTPS_PORT:-8443}"

usage() {
  cat <<'EOF'
Usage: scripts/dev-stack.sh [--tls] [--stop] [--down]

  (default)  Start Postgres + Keycloak, then Spring (dev) and Nuxt on the host.
  --tls      Also start Caddy HTTPS overlay (requires scripts/tls-lab-certs.sh first).
  --stop     Stop host Spring/Nuxt processes started by this script.
  --down     --stop plus docker compose down (keeps the Postgres volume).

HTTP URLs:  http://localhost:3000  (Nuxt)  http://localhost:8080/api/status
            http://localhost:8081  (Keycloak)

TLS URLs:   https://app.payment-quality.local:8443
            https://api.payment-quality.local:8443
            https://auth.payment-quality.local:8443
            (CADDY_HTTPS_PORT, default 8443 for rootless Podman)

Playwright live/POM on HTTP:
  PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=... PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=... \
    corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.config.ts

Canonical origin for POM is http://localhost:3000 (reuseExistingServer: true).
Live config starts its own Nuxt on :3000 unless a server is already bound.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tls) TLS=1 ;;
    --stop) STOP=1 ;;
    --down) DOWN=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
  shift
done

compose() {
  local files=(-f "$COMPOSE_FILE")
  if [[ "$TLS" == "1" ]]; then
    files+=(-f "$COMPOSE_TLS_FILE")
    export CADDY_HTTPS_PORT
  elif [[ "${1:-}" == "down" && -f "$COMPOSE_TLS_FILE" ]]; then
    files+=(-f "$COMPOSE_TLS_FILE")
  fi
  docker compose --env-file "$COMPOSE_ENV" "${files[@]}" "$@"
}

stop_host_apps() {
  for pid_file in "$BACKEND_PID_FILE" "$FRONTEND_PID_FILE"; do
    if [[ -f "$pid_file" ]]; then
      local pid
      pid="$(cat "$pid_file")"
      if kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
        wait "$pid" 2>/dev/null || true
      fi
      rm -f "$pid_file"
    fi
  done
  if command -v fuser >/dev/null 2>&1; then
    fuser -k 8080/tcp >/dev/null 2>&1 || true
    fuser -k 3000/tcp >/dev/null 2>&1 || true
  fi
}

wait_http() {
  local url="$1"
  local timeout_s="$2"
  local start
  start="$(date +%s)"
  while true; do
    if curl -sf "$url" >/dev/null 2>&1; then
      return 0
    fi
    if (( $(date +%s) - start > timeout_s )); then
      echo "Timed out waiting for $url" >&2
      return 1
    fi
    sleep 2
  done
}

if [[ "$STOP" == "1" || "$DOWN" == "1" ]]; then
  stop_host_apps
  if [[ "$DOWN" == "1" ]]; then
    if [[ ! -f "$COMPOSE_ENV" ]]; then
      echo "Missing $COMPOSE_ENV" >&2
      exit 1
    fi
    compose down
    rm -f "$LOG_DIR/tls.enabled"
  fi
  if [[ "$DOWN" == "1" ]]; then
    echo "Stopped host apps and compose."
  else
    echo "Stopped host apps."
  fi
  exit 0
fi

mkdir -p "$LOG_DIR"

if [[ ! -f "$COMPOSE_ENV" ]]; then
  cp "$REPO_ROOT/infra/compose/.env.example" "$COMPOSE_ENV"
  echo "Created $COMPOSE_ENV from example."
fi

if [[ "$TLS" == "1" ]]; then
  if [[ ! -f "$REPO_ROOT/infra/tls/cert.pem" || ! -f "$REPO_ROOT/infra/tls/key.pem" ]]; then
    echo "TLS certs missing. Run: scripts/tls-lab-certs.sh" >&2
    exit 1
  fi
  touch "$LOG_DIR/tls.enabled"
else
  rm -f "$LOG_DIR/tls.enabled"
fi

echo "Starting compose…"
compose up -d

echo "Waiting for Postgres…"
pg_start="$(date +%s)"
while true; do
  if compose exec -T payment-quality-postgres pg_isready -U payment_quality -d payment_quality_lab >/dev/null 2>&1; then
    break
  fi
  if (( $(date +%s) - pg_start > 90 )); then
    echo "Timed out waiting for Postgres" >&2
    exit 1
  fi
  sleep 2
done
echo "Waiting for Keycloak realm…"
wait_http "http://localhost:8081/realms/payment-quality" 180

stop_host_apps

SPRING_PROFILES="dev,seed"
if [[ "$TLS" == "1" ]]; then
  SPRING_PROFILES="dev,tls-lab,seed"
fi

echo "Starting Spring ($SPRING_PROFILES)…"
bash -c 'cd "$1" && exec env SPRING_PROFILES_ACTIVE="$2" ./mvnw spring-boot:run' \
  _ "$REPO_ROOT/apps/backend" "$SPRING_PROFILES" \
  >"$BACKEND_LOG" 2>&1 &
echo $! >"$BACKEND_PID_FILE"

echo "Waiting for /api/status…"
if ! wait_http "http://localhost:8080/api/status" 180; then
  echo "Spring failed to become ready. Last backend log lines:" >&2
  tail -n 80 "$BACKEND_LOG" >&2 || true
  exit 1
fi
if ! kill -0 "$(cat "$BACKEND_PID_FILE")" 2>/dev/null; then
  echo "Spring process exited. Last backend log lines:" >&2
  tail -n 80 "$BACKEND_LOG" >&2 || true
  exit 1
fi

NUXT_ENV=( )
if [[ "$TLS" == "1" ]]; then
  mkcert_ca="${MKCERT_CA:-}"
  if [[ -z "$mkcert_ca" && -x "$HOME/.local/bin/mkcert" ]]; then
    mkcert_ca="$("$HOME/.local/bin/mkcert" -CAROOT 2>/dev/null)/rootCA.pem"
  elif [[ -z "$mkcert_ca" ]] && command -v mkcert >/dev/null 2>&1; then
    mkcert_ca="$(mkcert -CAROOT 2>/dev/null)/rootCA.pem"
  fi
  NUXT_ENV=(
    NUXT_PUBLIC_KEYCLOAK_URL="https://auth.payment-quality.local:${CADDY_HTTPS_PORT}"
    NUXT_OAUTH_OIDC_OPENID_CONFIG=http://localhost:8081/realms/payment-quality/.well-known/openid-configuration
    NUXT_OAUTH_OIDC_REDIRECT_URL="https://app.payment-quality.local:${CADDY_HTTPS_PORT}/auth/keycloak"
    NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
    NODE_OPTIONS="--require ${REPO_ROOT}/scripts/tls-lab-node-preload.cjs"
  )
  if [[ -f "$mkcert_ca" ]]; then
    NUXT_ENV+=(NODE_EXTRA_CA_CERTS="$mkcert_ca")
  fi
fi

echo "Starting Nuxt…"
# HTTP stack binds loopback. TLS overlay binds 0.0.0.0 so Caddy in rootless
# Podman (pasta / host.docker.internal) can reach the host process. That
# exposes the dashboard on the LAN — lab-only, never a production pattern.
NUXT_HOST="127.0.0.1"
if [[ "$TLS" == "1" ]]; then
  NUXT_HOST="0.0.0.0"
fi
if [[ ${#NUXT_ENV[@]} -gt 0 ]]; then
  nohup env "${NUXT_ENV[@]}" corepack pnpm --dir "$REPO_ROOT/apps/frontend" dev --host "$NUXT_HOST" --port 3000 \
    >"$FRONTEND_LOG" 2>&1 &
else
  nohup corepack pnpm --dir "$REPO_ROOT/apps/frontend" dev --host "$NUXT_HOST" --port 3000 \
    >"$FRONTEND_LOG" 2>&1 &
fi
echo $! >"$FRONTEND_PID_FILE"

echo "Waiting for Nuxt…"
wait_http "http://127.0.0.1:3000" 120

if [[ "$TLS" == "1" ]]; then
  echo "Waiting for Caddy HTTPS…"
  local_start="$(date +%s)"
  while true; do
    if curl -sk --resolve "app.payment-quality.local:${CADDY_HTTPS_PORT}:127.0.0.1" \
      -o /dev/null -w '%{http_code}' "https://app.payment-quality.local:${CADDY_HTTPS_PORT}" \
      | grep -Eq '^(200|301|302|303|307|308)$'; then
      break
    fi
    if (( $(date +%s) - local_start > 60 )); then
      echo "Timed out waiting for https://app.payment-quality.local:${CADDY_HTTPS_PORT} (rootless overlay uses 8443; set CADDY_HTTPS_PORT=443 if privileged)" >&2
      exit 1
    fi
    sleep 2
  done
fi

cat <<EOF
Stack is up.
  Nuxt     http://localhost:3000
  Spring   http://localhost:8080/api/status
  Keycloak http://localhost:8081/realms/payment-quality
  Logs     $LOG_DIR
  Stop     scripts/dev-stack.sh --stop
EOF
if [[ "$TLS" == "1" ]]; then
  cat <<EOF
  TLS      https://app.payment-quality.local:${CADDY_HTTPS_PORT}
           https://api.payment-quality.local:${CADDY_HTTPS_PORT}
           https://auth.payment-quality.local:${CADDY_HTTPS_PORT}
  Nuxt     bound on 0.0.0.0:3000 so Caddy (pasta) can reach the host — LAN-visible, lab only
  Certs    mkcert -install for Chromium trust; else PLAYWRIGHT_TLS_INSECURE=1
  POM TLS  PLAYWRIGHT_BASE_URL=https://app.payment-quality.local:${CADDY_HTTPS_PORT} \\
             corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.tls.config.ts
EOF
fi
