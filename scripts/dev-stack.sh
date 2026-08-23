#!/usr/bin/env bash
# Raise the local lab stack (Podman/Docker compose + optional host hot reload).
#
#   (default)  Postgres + Keycloak in compose; Spring and Nuxt on the host.
#   --app      All services in compose over HTTP (published :3000 / :8080 / :8081).
#   --tls      HTTPS overlay: Caddy → host Spring/Nuxt (hot reload).
#   --full     HTTPS prod-like: Caddy → Spring/Nuxt containers.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_ENV="$REPO_ROOT/infra/compose/.env"
COMPOSE_FILE="$REPO_ROOT/infra/compose/compose.yml"
COMPOSE_TLS_FILE="$REPO_ROOT/infra/compose/compose.tls.yml"
COMPOSE_APP_FILE="$REPO_ROOT/infra/compose/compose.app.yml"
COMPOSE_APP_HTTP_FILE="$REPO_ROOT/infra/compose/compose.app.http.yml"
COMPOSE_KAFKA_FILE="$REPO_ROOT/infra/compose/compose.kafka.yml"
LOG_DIR="$REPO_ROOT/tmp/dev-stack"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"

TLS=0
FULL=0
APP=0
KAFKA=0
STOP=0
DOWN=0
CADDY_HTTPS_PORT="${CADDY_HTTPS_PORT:-8443}"
KEYCLOAK_PORT="${KEYCLOAK_PORT:-8081}"

usage() {
  cat <<'EOF'
Usage: scripts/dev-stack.sh [--app] [--tls] [--full] [--kafka] [--stop] [--down]

  (default)  Postgres + Keycloak in Podman; Spring (dev,seed) and Nuxt on the host.
  --app      Real HTTP deploy: Spring + Nuxt images, no Caddy. POM on :3000.
  --kafka    Lab Kafka overlay (KRaft broker on 9092) + create lab.auditable-actions.v1 (host hybrid).
  --tls      Caddy HTTPS overlay in front of host Spring/Nuxt (hot reload).
  --full     Prod-like HTTPS: Caddy + Spring + Nuxt containers (no hot reload).
  --stop     Stop host Spring/Nuxt processes started by this script.
  --down     --stop plus compose down (keeps the Postgres volume).

Do not combine --app with --tls or --full. --kafka is a third mode (do not mix with --app/--full).

HTTP URLs:  http://127.0.0.1:3000  (Nuxt)  http://127.0.0.1:8080/api/status
            http://127.0.0.1:8081  (Keycloak issuer http://localhost:8081)

TLS URLs:   https://app.payment-quality.local:8443
            https://api.payment-quality.local:8443
            https://auth.payment-quality.local:8443
            (CADDY_HTTPS_PORT, default 8443 for rootless Podman)

Playwright live/POM origin must match NUXT_OAUTH_OIDC_REDIRECT_URL:
  host DX   PLAYWRIGHT_BASE_URL=http://localhost:3000
  --app     PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000  + PLAYWRIGHT_SKIP_WEBSERVER=1
            (or scripts/run-app-stack-tests.sh)
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --app) APP=1 ;;
    --tls) TLS=1 ;;
    --full) FULL=1; TLS=1 ;;
    --kafka) KAFKA=1 ;;
    --stop) STOP=1 ;;
    --down) DOWN=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
  shift
done

if [[ "$APP" == "1" && ( "$TLS" == "1" || "$FULL" == "1" ) ]]; then
  echo "--app is HTTP compose; do not combine with --tls or --full" >&2
  exit 1
fi
if [[ "$KAFKA" == "1" && ( "$APP" == "1" || "$FULL" == "1" ) ]]; then
  echo "--kafka is a third mode; do not combine with --app/--full" >&2
  exit 1
fi

compose() {
  local files=(-f "$COMPOSE_FILE")
  if [[ "${1:-}" == "down" ]]; then
    files+=(-f "$COMPOSE_TLS_FILE" -f "$COMPOSE_APP_FILE" -f "$COMPOSE_APP_HTTP_FILE" -f "$COMPOSE_KAFKA_FILE")
  else
    if [[ "$TLS" == "1" ]]; then
      files+=(-f "$COMPOSE_TLS_FILE")
      export CADDY_HTTPS_PORT
    fi
    if [[ "$FULL" == "1" ]]; then
      files+=(-f "$COMPOSE_APP_FILE")
      export CADDY_HTTPS_PORT
    fi
    if [[ "$APP" == "1" ]]; then
      files+=(-f "$COMPOSE_APP_HTTP_FILE")
    fi
    if [[ "$KAFKA" == "1" ]]; then
      files+=(-f "$COMPOSE_KAFKA_FILE")
    fi
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

# podman-compose cannot replace same-name containers created by another overlay
# (--full vs --app vs host DX). Remove edge/app containers before up.
remove_stale_app_containers() {
  local name
  for name in payment-quality-backend payment-quality-frontend payment-quality-caddy; do
    docker rm -f "$name" >/dev/null 2>&1 || true
  done
}

# podman-compose 1.6 + rootless pasta records PortBindings but does not always
# start rootlessport. Stop/start after the process is up binds the host ports
# (verified on Podman 5.8; `restart` immediately after `up` is too early).
rebind_overlay_ports() {
  local name
  for name in payment-quality-caddy payment-quality-backend payment-quality-frontend; do
    if docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null | grep -q true; then
      echo "Rebinding host ports for $name…"
      docker stop "$name" >/dev/null 2>&1 || true
      docker start "$name" >/dev/null 2>&1 || true
    fi
  done
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

# Nuxt health: 2xx/3xx only. HTTP 500 is retried until timeout — first-boot and
# pasta rebind can 500 briefly. A stuck 500 is diagnosed only after timeout.
# Host `.nuxt/dev` is the host `pnpm dev` bundle, not the --app/--full image.
wait_nuxt() {
  local url="$1"
  local timeout_s="$2"
  local start code last_code=000
  local nitro="$REPO_ROOT/apps/frontend/.nuxt/dev/index.mjs"
  start="$(date +%s)"
  while true; do
    code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 3 "$url" 2>/dev/null || echo 000)"
    last_code="$code"
    case "$code" in
      200|301|302|303|307|308)
        echo "nuxt $url -> $code"
        return 0
        ;;
    esac
    if (( $(date +%s) - start > timeout_s )); then
      echo "Timed out waiting for Nuxt $url (last HTTP $last_code)" >&2
      if [[ "$APP" == "1" || "$FULL" == "1" ]]; then
        echo "Compose frontend, not host .nuxt/dev." >&2
        docker logs --tail 40 payment-quality-frontend >&2 || true
        if [[ "$last_code" == "500" && "$APP" == "1" ]]; then
          echo "Fix: scripts/dev-stack.sh --down && scripts/dev-stack.sh --app" >&2
          echo "Host pnpm dev on :3000? scripts/dev-stack.sh --stop first." >&2
        elif [[ "$last_code" == "500" ]]; then
          echo "Fix: scripts/dev-stack.sh --down && scripts/dev-stack.sh --full" >&2
        fi
      else
        if [[ "$last_code" == "500" && -f "$nitro" ]]; then
          echo "Host Nitro bundle is not valid ESM (or still booting past timeout)." >&2
          node --check "$nitro" >&2 || true
        fi
        tail -n 40 "$FRONTEND_LOG" >&2 || true
        if [[ "$last_code" == "500" ]]; then
          echo "Fix: scripts/dev-stack.sh --stop && rm -rf apps/frontend/.nuxt/dev && scripts/dev-stack.sh" >&2
        fi
      fi
      return 1
    fi
    sleep 2
  done
}

env_value() {
  local key="$1"
  grep -E "^${key}=" "$COMPOSE_ENV" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' | tr -d "'" || true
}

require_session_password() {
  local session_pw
  session_pw="$(env_value NUXT_SESSION_PASSWORD)"
  if [[ ${#session_pw} -lt 32 ]]; then
    echo "NUXT_SESSION_PASSWORD in $COMPOSE_ENV must be at least 32 characters before compose --app/--full" >&2
    exit 1
  fi
}

wait_keycloak_issuer() {
  local expected_prefix="$1"
  local discovery="http://127.0.0.1:${KEYCLOAK_PORT}/realms/payment-quality/.well-known/openid-configuration"
  echo "Waiting for Keycloak realm…"
  wait_http "http://127.0.0.1:${KEYCLOAK_PORT}/realms/payment-quality" 180
  if ! "$REPO_ROOT/scripts/keycloak-issuer-oracle.sh" "$expected_prefix" "$discovery"; then
    echo "Recreating Keycloak so hostname matches this stack mode…"
    compose up -d --force-recreate --no-deps payment-quality-keycloak
    wait_http "http://127.0.0.1:${KEYCLOAK_PORT}/realms/payment-quality" 180
    "$REPO_ROOT/scripts/keycloak-issuer-oracle.sh" "$expected_prefix" "$discovery"
  fi
}

if [[ "$STOP" == "1" || "$DOWN" == "1" ]]; then
  stop_host_apps
  if [[ "$DOWN" == "1" ]]; then
    if [[ ! -f "$COMPOSE_ENV" ]]; then
      echo "Missing $COMPOSE_ENV" >&2
      exit 1
    fi
    compose down --remove-orphans
    rm -f "$LOG_DIR/tls.enabled" "$LOG_DIR/full.enabled" "$LOG_DIR/app.enabled"
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

port_from_env="$(env_value KEYCLOAK_PORT)"
if [[ -n "$port_from_env" ]]; then
  KEYCLOAK_PORT="$port_from_env"
fi

if [[ "$TLS" == "1" ]]; then
  if [[ ! -f "$REPO_ROOT/infra/tls/cert.pem" || ! -f "$REPO_ROOT/infra/tls/key.pem" ]]; then
    echo "TLS certs missing. Run: scripts/tls-lab-certs.sh" >&2
    exit 1
  fi
  touch "$LOG_DIR/tls.enabled"
  if [[ ! -f "$REPO_ROOT/infra/tls/rootCA.pem" ]]; then
    mkcert_bin="mkcert"
    if [[ -x "$HOME/.local/bin/mkcert" ]]; then
      mkcert_bin="$HOME/.local/bin/mkcert"
    fi
    if command -v "$mkcert_bin" >/dev/null 2>&1 || [[ -x "$mkcert_bin" ]]; then
      cp "$("$mkcert_bin" -CAROOT)/rootCA.pem" "$REPO_ROOT/infra/tls/rootCA.pem"
    fi
  fi
else
  rm -f "$LOG_DIR/tls.enabled"
fi
if [[ "$FULL" == "1" ]]; then
  require_session_password
  if [[ ! -f "$REPO_ROOT/infra/tls/rootCA.pem" ]]; then
    echo "mkcert CA copy missing at infra/tls/rootCA.pem (run scripts/tls-lab-certs.sh)" >&2
    exit 1
  fi
  touch "$LOG_DIR/full.enabled"
else
  rm -f "$LOG_DIR/full.enabled"
fi
if [[ "$APP" == "1" ]]; then
  require_session_password
  touch "$LOG_DIR/app.enabled"
else
  rm -f "$LOG_DIR/app.enabled"
fi

echo "Starting compose…"
stop_host_apps
remove_stale_app_containers
if [[ "$FULL" == "1" || "$APP" == "1" ]]; then
  compose build
fi
if [[ "$APP" == "1" || "$FULL" == "1" ]]; then
  compose up -d --force-recreate --remove-orphans
else
  compose up -d --remove-orphans
fi

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

if [[ "$TLS" == "1" ]]; then
  wait_keycloak_issuer "https://auth.payment-quality.local:${CADDY_HTTPS_PORT}"
else
  wait_keycloak_issuer "http://localhost:${KEYCLOAK_PORT}"
fi

if [[ "$TLS" == "1" && "$FULL" != "1" ]]; then
  rebind_overlay_ports
fi

if [[ "$KAFKA" == "1" ]]; then
  echo "Waiting for Kafka broker…"
  kafka_start="$(date +%s)"
  while true; do
    if docker exec payment-quality-kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/dev/null 2>&1; then
      break
    fi
    if (( $(date +%s) - kafka_start > 90 )); then
      echo "Timed out waiting for Kafka" >&2
      docker logs --tail 80 payment-quality-kafka >&2 || true
      exit 1
    fi
    sleep 2
  done
  echo "Ensuring topic lab.auditable-actions.v1 (3 partitions RF1)…"
  if ! docker exec payment-quality-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic lab.auditable-actions.v1 --partitions 3 --replication-factor 1 >/dev/null 2>&1; then
    echo "Failed to create lab.auditable-actions.v1" >&2
    docker logs --tail 40 payment-quality-kafka >&2 || true
    exit 1
  fi
  echo "Ensuring DLT topic lab.event-lab.dlq.v1…"
  docker exec payment-quality-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic lab.event-lab.dlq.v1 --partitions 3 --replication-factor 1 >/dev/null 2>&1 || true
  touch "$LOG_DIR/kafka.enabled"
else
  rm -f "$LOG_DIR/kafka.enabled"
fi

stop_host_apps

if [[ "$FULL" == "1" ]]; then
  echo "Waiting for Spring in compose…"
  app_start="$(date +%s)"
  while true; do
    if compose exec -T payment-quality-backend curl -sf "http://127.0.0.1:8080/api/status" >/dev/null 2>&1; then
      break
    fi
    if (( $(date +%s) - app_start > 180 )); then
      echo "Backend container failed to become ready." >&2
      compose logs --tail=80 payment-quality-backend >&2 || true
      exit 1
    fi
    sleep 3
  done
  echo "Waiting for Nuxt in compose…"
  fe_start="$(date +%s)"
  while true; do
    if compose exec -T payment-quality-frontend \
      node -e "fetch('http://127.0.0.1:3000').then((r)=>process.exit(r.status<500?0:1)).catch(()=>process.exit(1))" \
      >/dev/null 2>&1; then
      break
    fi
    if (( $(date +%s) - fe_start > 180 )); then
      echo "Frontend container failed to become ready." >&2
      compose logs --tail=80 payment-quality-frontend >&2 || true
      exit 1
    fi
    sleep 3
  done
  echo "Rebinding published host ports (podman-compose + pasta)…"
  rebind_overlay_ports
  echo "Waiting for Caddy HTTPS (full compose)…"
  local_start="$(date +%s)"
  while true; do
    if curl -sk --resolve "app.payment-quality.local:${CADDY_HTTPS_PORT}:127.0.0.1" \
      -o /dev/null -w '%{http_code}' "https://app.payment-quality.local:${CADDY_HTTPS_PORT}" \
      | grep -Eq '^(200|301|302|303|307|308)$'; then
      break
    fi
    if (( $(date +%s) - local_start > 240 )); then
      echo "Timed out waiting for full-stack https://app.payment-quality.local:${CADDY_HTTPS_PORT}" >&2
      compose logs --tail=80 payment-quality-backend payment-quality-frontend payment-quality-caddy >&2 || true
      exit 1
    fi
    sleep 3
  done
  cat <<EOF
Full HTTPS stack is up (Spring and Nuxt in compose).
  App      https://app.payment-quality.local:${CADDY_HTTPS_PORT}
  API      https://api.payment-quality.local:${CADDY_HTTPS_PORT}
  Auth     https://auth.payment-quality.local:${CADDY_HTTPS_PORT}
  Keycloak http://127.0.0.1:${KEYCLOAK_PORT} (JWKS / admin; issuer is the HTTPS auth vhost)
  Rebuild  scripts/dev-stack.sh --full
  Stop     scripts/dev-stack.sh --down
  POM TLS  PLAYWRIGHT_SKIP_WEBSERVER=1 \\
             PLAYWRIGHT_BASE_URL=https://app.payment-quality.local:${CADDY_HTTPS_PORT} \\
             corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.tls.config.ts
EOF
  exit 0
fi

if [[ "$APP" == "1" ]]; then
  echo "Waiting for Spring in compose…"
  app_start="$(date +%s)"
  while true; do
    if compose exec -T payment-quality-backend curl -sf "http://127.0.0.1:8080/api/status" >/dev/null 2>&1; then
      break
    fi
    if (( $(date +%s) - app_start > 180 )); then
      echo "Backend container failed to become ready." >&2
      compose logs --tail=80 payment-quality-backend >&2 || true
      exit 1
    fi
    sleep 3
  done
  echo "Waiting for Nuxt in compose…"
  fe_start="$(date +%s)"
  while true; do
    if compose exec -T payment-quality-frontend \
      node -e "fetch('http://127.0.0.1:3000').then((r)=>process.exit(r.status<500?0:1)).catch(()=>process.exit(1))" \
      >/dev/null 2>&1; then
      break
    fi
    if (( $(date +%s) - fe_start > 180 )); then
      echo "Frontend container failed to become ready." >&2
      compose logs --tail=80 payment-quality-frontend >&2 || true
      exit 1
    fi
    sleep 3
  done
  echo "Rebinding published host ports (podman-compose + pasta)…"
  rebind_overlay_ports
  echo "Waiting for Spring on the host…"
  app_start="$(date +%s)"
  while true; do
    if compose exec -T payment-quality-backend curl -sf "http://127.0.0.1:8080/api/status" >/dev/null 2>&1; then
      break
    fi
    if (( $(date +%s) - app_start > 120 )); then
      echo "Backend did not recover after port rebind." >&2
      compose logs --tail=40 payment-quality-backend >&2 || true
      exit 1
    fi
    sleep 2
  done
  fe_start="$(date +%s)"
  while true; do
    if compose exec -T payment-quality-frontend \
      node -e "fetch('http://127.0.0.1:3000').then((r)=>process.exit(r.status<500?0:1)).catch(()=>process.exit(1))" \
      >/dev/null 2>&1; then
      break
    fi
    if (( $(date +%s) - fe_start > 120 )); then
      echo "Frontend did not recover after port rebind." >&2
      compose logs --tail=40 payment-quality-frontend >&2 || true
      exit 1
    fi
    sleep 2
  done
  if ! wait_http "http://127.0.0.1:8080/api/status" 45 || ! wait_nuxt "http://127.0.0.1:3000" 45; then
    echo "Containers are healthy but host :3000/:8080 are not reachable (rootless pasta) or Nuxt is 500." >&2
    echo "Check: ss -tlnp | grep -E ':3000|:8080' — rootlessport must listen." >&2
    echo "Host nuxt.dev holding :3000? scripts/dev-stack.sh --stop first." >&2
    echo "Retry: scripts/dev-stack.sh --down && scripts/dev-stack.sh --app" >&2
    exit 1
  fi
  cat <<EOF
HTTP compose stack is up (Spring and Nuxt in Podman).
  Nuxt     http://127.0.0.1:3000
  Spring   http://127.0.0.1:8080/api/status
  Keycloak http://127.0.0.1:${KEYCLOAK_PORT}  (issuer http://localhost:${KEYCLOAK_PORT})
  Rebuild  scripts/dev-stack.sh --app
  Stop     scripts/dev-stack.sh --down
  POM HTTP PLAYWRIGHT_SKIP_WEBSERVER=1 \\
             PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000 \\
             corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.config.ts
EOF
  exit 0
fi

SPRING_PROFILES="dev,seed"
if [[ "$TLS" == "1" ]]; then
  SPRING_PROFILES="dev,tls-lab,seed"
fi
if [[ "$KAFKA" == "1" ]]; then
  SPRING_PROFILES="${SPRING_PROFILES},kafka"
fi

APP_EVENT_LAB_ENABLED="true"
if [[ "$KAFKA" == "1" ]]; then
  export APP_EVENT_LAB_ENABLED
fi

echo "Starting Spring ($SPRING_PROFILES)…"
bash -c 'cd "$1" && exec env SPRING_PROFILES_ACTIVE="$2" APP_EVENT_LAB_ENABLED="$3" ./mvnw spring-boot:run' \
  _ "$REPO_ROOT/apps/backend" "$SPRING_PROFILES" "$APP_EVENT_LAB_ENABLED" \
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

NUXT_ENV=( NUXT_TYPECHECK=false )
if [[ "$TLS" == "1" ]]; then
  mkcert_ca="${MKCERT_CA:-}"
  if [[ -z "$mkcert_ca" && -x "$HOME/.local/bin/mkcert" ]]; then
    mkcert_ca="$("$HOME/.local/bin/mkcert" -CAROOT 2>/dev/null)/rootCA.pem"
  elif [[ -z "$mkcert_ca" ]] && command -v mkcert >/dev/null 2>&1; then
    mkcert_ca="$(mkcert -CAROOT 2>/dev/null)/rootCA.pem"
  fi
  NUXT_ENV=(
    NUXT_PUBLIC_KEYCLOAK_URL="https://auth.payment-quality.local:${CADDY_HTTPS_PORT}"
    NUXT_OAUTH_OIDC_OPENID_CONFIG="http://localhost:${KEYCLOAK_PORT}/realms/payment-quality/.well-known/openid-configuration"
    NUXT_OAUTH_OIDC_REDIRECT_URL="https://app.payment-quality.local:${CADDY_HTTPS_PORT}/auth/keycloak"
    NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
    NODE_OPTIONS="--require ${REPO_ROOT}/scripts/tls-lab-node-preload.cjs"
    NUXT_SESSION_COOKIE_SECURE=true
  )
  if [[ -f "$mkcert_ca" ]]; then
    NUXT_ENV+=(NODE_EXTRA_CA_CERTS="$mkcert_ca")
  fi
fi
NUXT_ENV+=(NUXT_TYPECHECK=false)

echo "Starting Nuxt…"
# Disposable Nitro output: a crashed HMR rewrite leaves Unexpected token ')' on GET /.
rm -rf "$REPO_ROOT/apps/frontend/.nuxt/dev"
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
wait_nuxt "http://127.0.0.1:3000" 120

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
  Nuxt     http://127.0.0.1:3000
  Spring   http://127.0.0.1:8080/api/status
  Keycloak http://127.0.0.1:${KEYCLOAK_PORT}/realms/payment-quality
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
