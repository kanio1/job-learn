#!/usr/bin/env python3
"""Ensure merchant.manager.w0–w3 exist with tenant_id / merchant_id claims.

Keycloak --import-realm does not update an existing volume. Keycloak 26 user
profile drops unmanaged attributes on Admin API create unless they are declared.
"""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.parse
import urllib.request

KC = "http://127.0.0.1:8081"
REALM = "payment-quality"
CLAIM_ATTRS = (
    {
        "name": "tenant_id",
        "displayName": "Tenant id",
        "permissions": {"view": ["admin", "user"], "edit": ["admin"]},
        "multivalued": False,
    },
    {
        "name": "merchant_id",
        "displayName": "Merchant id",
        "permissions": {"view": ["admin", "user"], "edit": ["admin"]},
        "multivalued": False,
    },
)


def req(method: str, url: str, data=None, token: str | None = None, form: bool = False):
    headers: dict[str, str] = {}
    body = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if data is not None:
        if form:
            body = urllib.parse.urlencode(data).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            body = json.dumps(data).encode()
            headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        raw = error.read()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw.decode(errors="replace")
        return error.code, parsed


def main() -> int:
    status, token_body = req("POST", f"{KC}/realms/master/protocol/openid-connect/token", {
        "username": "admin",
        "password": "admin",
        "grant_type": "password",
        "client_id": "admin-cli",
    }, form=True)
    if status != 200 or not isinstance(token_body, dict):
        print("admin token failed", status, token_body, file=sys.stderr)
        return 1
    token = token_body["access_token"]

    status, profile = req("GET", f"{KC}/admin/realms/{REALM}/users/profile", token=token)
    if status != 200 or not isinstance(profile, dict):
        print("user profile GET failed", status, profile, file=sys.stderr)
        return 1
    names = {attr.get("name") for attr in profile.get("attributes") or []}
    changed = False
    for attr in CLAIM_ATTRS:
        if attr["name"] not in names:
            profile.setdefault("attributes", []).append(attr)
            changed = True
    if profile.get("unmanagedAttributePolicy") not in {"ENABLED", "ADMIN_EDIT", "ANY"}:
        profile["unmanagedAttributePolicy"] = "ADMIN_EDIT"
        changed = True
    if changed:
        status, put = req("PUT", f"{KC}/admin/realms/{REALM}/users/profile", profile, token=token)
        if status not in (200, 204):
            print("user profile PUT failed", status, put, file=sys.stderr)
            return 1

    status, role = req("GET", f"{KC}/admin/realms/{REALM}/roles/MERCHANT_MANAGER", token=token)
    if status != 200 or not isinstance(role, dict):
        print("MERCHANT_MANAGER role missing", status, role, file=sys.stderr)
        return 1

    for index in range(4):
        username = f"merchant.manager.w{index}"
        merchant_id = f"00000000-0000-0000-0000-0000000000d{index}"
        payload = {
            "username": username,
            "enabled": True,
            "emailVerified": True,
            "firstName": "Merchant",
            "lastName": f"Manager W{index}",
            "email": f"{username}@example.test",
            "attributes": {
                "merchant_id": [merchant_id],
                "tenant_id": ["TENANT_ALPHA"],
            },
        }
        status, users = req(
            "GET",
            f"{KC}/admin/realms/{REALM}/users?username={username}&exact=true",
            token=token,
        )
        existing = users[0] if status == 200 and isinstance(users, list) and users else None
        if existing:
            uid = existing["id"]
            existing.update(payload)
            status, _ = req("PUT", f"{KC}/admin/realms/{REALM}/users/{uid}", existing, token=token)
            if status not in (204, 200):
                print("update failed", username, status, file=sys.stderr)
                return 1
        else:
            status, _ = req("POST", f"{KC}/admin/realms/{REALM}/users", payload, token=token)
            if status not in (201, 409):
                print("create failed", username, status, file=sys.stderr)
                return 1
            status, users = req(
                "GET",
                f"{KC}/admin/realms/{REALM}/users?username={username}&exact=true",
                token=token,
            )
            uid = users[0]["id"]
        req("PUT", f"{KC}/admin/realms/{REALM}/users/{uid}/reset-password", {
            "type": "password",
            "value": username,
            "temporary": False,
        }, token=token)
        req("POST", f"{KC}/admin/realms/{REALM}/users/{uid}/role-mappings/realm", [role], token=token)
        print(username, merchant_id)

    print("ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
