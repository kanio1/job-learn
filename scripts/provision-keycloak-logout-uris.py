#!/usr/bin/env python3
"""Set post.logout.redirect.uris on payment-quality-dashboard.

Keycloak --import-realm does not update an existing volume. Additive only:
does not change roles or users.
"""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.parse
import urllib.request

KC = "http://127.0.0.1:8081"
REALM = "payment-quality"
CLIENT_ID = "payment-quality-dashboard"
LOGOUT_URIS = "##".join(
    (
        "http://localhost:3000/login",
        "http://127.0.0.1:3000/login",
        "https://app.payment-quality.local:8443/login",
        "https://app.payment-quality.local/login",
    )
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

    status, clients = req(
        "GET",
        f"{KC}/admin/realms/{REALM}/clients?clientId={CLIENT_ID}",
        token=token,
    )
    if status != 200 or not isinstance(clients, list) or not clients:
        print("client GET failed", status, clients, file=sys.stderr)
        return 1
    client = clients[0]
    cid = client["id"]
    attrs = dict(client.get("attributes") or {})
    if attrs.get("post.logout.redirect.uris") == LOGOUT_URIS:
        print("ok (unchanged)")
        return 0
    attrs["post.logout.redirect.uris"] = LOGOUT_URIS
    client["attributes"] = attrs
    status, put = req("PUT", f"{KC}/admin/realms/{REALM}/clients/{cid}", client, token=token)
    if status not in (200, 204):
        print("client PUT failed", status, put, file=sys.stderr)
        return 1
    print("ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
