# TLS lab overlay (FR-TLS-LAB)

Local HTTPS for the dashboard, API, and Keycloak. **Default compose stays HTTP.** Certs are mkcert material and are gitignored.

## Hosts

Add to `/etc/hosts` (requires local admin):

```text
127.0.0.1 app.payment-quality.local api.payment-quality.local auth.payment-quality.local
```

Playwright TLS POM also maps those names with Chromium `--host-resolver-rules`, so the suite can run without editing `/etc/hosts`. `curl` health checks in `scripts/dev-stack.sh --tls` use `--resolve`.

## Certs

```bash
scripts/tls-lab-certs.sh
```

Requires [mkcert](https://github.com/FiloSottile/mkcert). Writes `infra/tls/cert.pem` and `infra/tls/key.pem`.

## Start

```bash
scripts/dev-stack.sh --tls
```

This starts Postgres + Keycloak (`start` behind Caddy) + Caddy, then Spring `dev,tls-lab` and Nuxt on the host.

**Nuxt bind:** HTTP stack uses `127.0.0.1:3000`. TLS overlay uses `0.0.0.0:3000` so Caddy in rootless Podman (pasta / `host.docker.internal`) can reach the host process. That makes the dashboard reachable on the LAN. Lab-only — not a production bind.

Rootless Podman cannot bind privileged port 443, so the overlay **defaults to host 8443 → container 443** (`CADDY_HTTPS_PORT`). Set `CADDY_HTTPS_PORT=443` only when the host allows it.

| Name | URL |
|---|---|
| App | `https://app.payment-quality.local:8443` |
| API (via Caddy) | `https://api.payment-quality.local:8443` |
| Keycloak | `https://auth.payment-quality.local:8443` |
| Spring still on host | `http://localhost:8080` |
| Keycloak HTTP (JWKS) | `http://localhost:8081` |

Spring validates JWT `iss` as `https://auth.payment-quality.local:8443/realms/payment-quality` and fetches JWKS from localhost HTTP so the JVM does not need the mkcert CA.

Nuxt browser OIDC redirect is `https://app.payment-quality.local:8443/auth/keycloak`. Recreate the Keycloak container after realm JSON changes so additive HTTPS redirect URIs import. Host Nuxt maps those names via `scripts/tls-lab-node-preload.cjs` (no `/etc/hosts` required for token exchange).

## Playwright

Chromium trusts mkcert only after `mkcert -install` (OS trust store). `NODE_EXTRA_CA_CERTS` helps Node (`BffClient`), not the browser. `playwright.pom.tls.config.ts` loads the mkcert CA for Node and sets `ignoreHTTPSErrors` **only** when `PLAYWRIGHT_TLS_INSECURE=1`.

```bash
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
# Prefer: mkcert -install
# Without OS trust (no sudo): PLAYWRIGHT_TLS_INSECURE=1
corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.tls.config.ts
```

A green suite with `PLAYWRIGHT_TLS_INSECURE=1` does **not** prove the browser trusts the local CA. `curl --cacert "$(mkcert -CAROOT)/rootCA.pem"` is the cert oracle.

## Stop

```bash
scripts/dev-stack.sh --down
```
