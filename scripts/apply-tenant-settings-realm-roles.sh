#!/usr/bin/env bash
# Add tenant-settings leaves to a running Keycloak (realm JSON only applies on first import).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$ROOT/infra/compose/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT/infra/compose/.env"
  set +a
fi
export KEYCLOAK_URL="${KEYCLOAK_URL:-http://127.0.0.1:8081}"
export KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
export KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
python3 - <<'PY'
import json, os, urllib.error, urllib.parse, urllib.request

kc = os.environ["KEYCLOAK_URL"].rstrip("/")
admin = os.environ["KEYCLOAK_ADMIN"]
password = os.environ["KEYCLOAK_ADMIN_PASSWORD"]
realm = "payment-quality"
batches = [
    (
        ["platform:tenant:settings:read", "platform:tenant:settings:update"],
        ["PLATFORM_ADMIN", "TENANT_ADMIN"],
    ),
    (
        ["platform:payments:notes:read", "platform:payments:notes:create"],
        ["PLATFORM_ADMIN", "SUPPORT_AGENT"],
    ),
]

token_req = urllib.request.Request(
    kc + "/realms/master/protocol/openid-connect/token",
    data=urllib.parse.urlencode({
        "grant_type": "password",
        "client_id": "admin-cli",
        "username": admin,
        "password": password,
    }).encode(),
)
with urllib.request.urlopen(token_req) as response:
    token = json.load(response)["access_token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def get(path):
    req = urllib.request.Request(kc + path, headers=headers)
    with urllib.request.urlopen(req) as response:
        return json.load(response)

def post(path, payload):
    req = urllib.request.Request(
        kc + path,
        data=json.dumps(payload).encode(),
        headers=headers,
        method="POST",
    )
    with urllib.request.urlopen(req) as response:
        return response.status

for leaves, composites in batches:
    roles = []
    for name in leaves:
        try:
            roles.append(get(f"/admin/realms/{realm}/roles/{name}"))
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
            post(f"/admin/realms/{realm}/roles", {"name": name})
            roles.append(get(f"/admin/realms/{realm}/roles/{name}"))

    for composite in composites:
        existing = {role["name"] for role in get(f"/admin/realms/{realm}/roles/{composite}/composites")}
        missing = [role for role in roles if role["name"] not in existing]
        if missing:
            post(f"/admin/realms/{realm}/roles/{composite}/composites", missing)
        print(f"ok {composite} {', '.join(leaves)}")
PY
