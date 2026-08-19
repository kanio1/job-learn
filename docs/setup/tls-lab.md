# TLS lab overlay (FR-TLS-LAB)

Local HTTPS for the dashboard, API, and Keycloak. **Default compose stays HTTP.** Certs are mkcert material and are gitignored.

Operator runbook (HTTP `--app`, HTTPS `--full`, POM, Caddy vs ACME): [run-stack-and-pom.md](run-stack-and-pom.md).

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

**Four modes:**

| Command | What runs | Ingress | POM origin |
|---|---|---|---|
| `scripts/dev-stack.sh` | host Spring/Nuxt | HTTP `:3000` | Playwright `http://localhost:3000` (curl `127.0.0.1`) |
| `scripts/dev-stack.sh --app` | Spring/Nuxt images | HTTP `:3000` | same + `PLAYWRIGHT_SKIP_WEBSERVER=1` |
| `scripts/dev-stack.sh --tls` | host + Caddy | HTTPS `:8443` | `playwright.pom.tls.config.ts` |
| `scripts/dev-stack.sh --full` | images + Caddy | HTTPS `:8443` | TLS config + `PLAYWRIGHT_SKIP_WEBSERVER=1` |

`--app` / `--full` rebuild images; they are not hot reload. Mode switches recreate Keycloak when the advertised OIDC issuer does not match (`scripts/keycloak-issuer-oracle.sh`).

**Caddy TLS:** `.local` names are not eligible for Let's Encrypt ([Automatic HTTPS hostname requirements](https://caddyserver.com/docs/automatic-https#hostname-requirements)). Local lab uses mkcert files mounted into Caddy (`tls /certs/cert.pem`). Caddy still applies its edge policy: `encode gzip zstd`, `request_body` 5MB, security headers, short lab HSTS (`max-age=300`), `/__oidc*` 404 on `--full`. Production ACME is `infra/caddy/Caddyfile.prod.example` (public DNS + `CADDY_ACME_EMAIL`). Caddy `tls internal` would still require installing Caddy's local CA into the host/NSS store — same operator-trust class as `mkcert -install`, worse Playwright DX — so the lab keeps mkcert.

**Keycloak hostname:** HTTP compose sets `KC_HOSTNAME=http://localhost:8081` and `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true` ([Keycloak hostname v2](https://www.keycloak.org/server/hostname)). TLS overlays override hostname to `https://auth.payment-quality.local:8443`. `--full` OIDC rewrite at `/__oidc` remains loopback-only.

**Nuxt bind:** HTTP stack uses `127.0.0.1:3000`. TLS overlay uses `0.0.0.0:3000` so Caddy in rootless Podman (pasta / `host.docker.internal`) can reach the host process. That makes the dashboard reachable on the LAN. Lab-only — not a production bind. `--app` publishes `:3000`. `--full` binds Nuxt only inside the compose network.

**Session cookie:** `--tls` / `--full` set `NUXT_SESSION_COOKIE_SECURE=true`. `--app` keeps it false. Cookie `Secure` follows that env only (not `NODE_ENV=production`). `--app` and `--full` require `NUXT_SESSION_PASSWORD` ≥32.

Rootless Podman cannot bind privileged port 443, so the overlay **defaults to host 8443 → container 443** (`CADDY_HTTPS_PORT`). Set `CADDY_HTTPS_PORT=443` only when the host allows it.

| Name | URL |
|---|---|
| App | `https://app.payment-quality.local:8443` |
| API (via Caddy) | `https://api.payment-quality.local:8443` |
| Keycloak | `https://auth.payment-quality.local:8443` |
| Spring still on host | `http://localhost:8080` |
| Keycloak HTTP (JWKS) | `http://localhost:8081` |

Spring validates JWT `iss` as `https://auth.payment-quality.local:8443/realms/payment-quality` and fetches JWKS from Keycloak HTTP so the JVM does not need the mkcert CA.

**`--full` OIDC split:** Nuxt serves rewritten discovery at `/__oidc/openid-configuration` only on loopback (`Host` `127.0.0.1` / `localhost`). Caddy `app.` returns **404** for `/__oidc*` (no docker DNS leak). `NUXT_OAUTH_OIDC_OPENID_CONFIG=http://127.0.0.1:3000/__oidc/openid-configuration`. `issuer` and `authorization_endpoint` stay `https://auth.payment-quality.local:8443/...`; back-channel fields (`token_endpoint`, `jwks_uri`, `userinfo_endpoint`, `end_session_endpoint`, `revocation_endpoint`, `introspection_endpoint`, PAR, CIBA) use `http://payment-quality-keycloak:8080/...`. Oracle: `scripts/tls-lab-oidc-backchannel-oracle.sh`. The frontend container does not hairpin HTTPS through Caddy.

`--full` requires `NUXT_SESSION_PASSWORD` (≥32) in `infra/compose/.env` before `compose up`; Nitro throws at startup when `NUXT_SESSION_COOKIE_SECURE=true` and the password is missing or short.

Hybrid `--tls` keeps host Nuxt discovery at `http://localhost:8081/realms/payment-quality/.well-known/openid-configuration`. Recreate the Keycloak container after realm JSON changes so additive HTTPS redirect URIs import. Host Nuxt maps those names via `scripts/tls-lab-node-preload.cjs` (no `/etc/hosts` required for token exchange).

## Playwright

Chromium trusts mkcert only after `mkcert -install` (OS trust store; often sudo). On Linux, Chromium also uses NSS (`~/.pki/nssdb`). If `mkcert -install` still leaves `ERR_CERT_AUTHORITY_INVALID`, install `nss-tools` / `libnss3-tools` and run `mkcert -install` again. `NODE_EXTRA_CA_CERTS` helps Node (`BffClient`), not the browser. `playwright.pom.tls.config.ts` loads the mkcert CA for Node and sets `ignoreHTTPSErrors` **only** when `PLAYWRIGHT_TLS_INSECURE=1`.

```bash
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
# Prefer: mkcert -install
# Without OS trust (no sudo): PLAYWRIGHT_TLS_INSECURE=1
corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.tls.config.ts
```

A green suite with `PLAYWRIGHT_TLS_INSECURE=1` does **not** prove the browser trusts the local CA. Cert oracle:

```bash
scripts/tls-lab-cert-oracle.sh
PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager scripts/tls-lab-oidc-backchannel-oracle.sh
```

Live Location / Vary through Caddy (RA-RFC-031 / 034):

```bash
PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager scripts/tls-lab-location-oracle.sh
```

## Full compose (Wave 3)

Spring and Nuxt run in containers. Caddy uses [Caddyfile.full](../../infra/caddy/Caddyfile.full) (`app` → frontend, `api` → backend). Recreate Keycloak after realm JSON changes.

```bash
scripts/tls-lab-certs.sh
scripts/dev-stack.sh --full
```

Daily DX stays `scripts/dev-stack.sh` (HTTP, hot reload) or `scripts/dev-stack.sh --app` (HTTP images, no Caddy). `--full` requires an image rebuild after app changes.

Rootless Podman (pasta) often does **not** expose Spring's published `:8080` on host loopback even when `docker port` lists it. `scripts/tls-lab-location-oracle.sh` then dual-compares Caddy with `docker exec payment-quality-backend curl http://127.0.0.1:8080`.

TLS POM: export `PLAYWRIGHT_SKIP_WEBSERVER=1` (printed by `dev-stack.sh --full`) so Playwright does not start host `pnpm dev`. `webServer.ignoreHTTPSErrors` follows `PLAYWRIGHT_TLS_INSECURE=1` only.

Frontend image runs `nuxi prepare` then `pnpm typecheck` before `pnpm build`. Compose `--full` requires `NUXT_SESSION_PASSWORD` in `infra/compose/.env` (no default). Backend image builds with `./mvnw` from the repo wrapper.

## Stop

```bash
scripts/dev-stack.sh --down
```
