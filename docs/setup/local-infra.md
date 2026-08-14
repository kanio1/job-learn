# Local Infrastructure Setup

Phase 0 defines local PostgreSQL 18 and Keycloak 26.6.1 infrastructure so later backend, security, and testing work has a known baseline. Compose uses fully qualified image names to avoid non-interactive short-name resolution failures in Podman-backed Docker environments.

## Environment File

Create a local `.env` from the committed example:

```bash
cp infra/compose/.env.example infra/compose/.env
```

The example contains non-secret local values only:
- PostgreSQL database, user, password, and port
- Keycloak admin username, password, and port

Production secrets, business realm variables, and application OAuth/OIDC client variables are intentionally deferred.

## Full local stack (HTTP)

Postgres and Keycloak stay in Compose. Spring and Nuxt stay on the host (hot reload).

```bash
scripts/dev-stack.sh
```

This waits for Postgres, the Keycloak realm, `GET /api/status`, and Nuxt `:3000`. Spring starts with `dev,seed` so `MERCHANT_ALPHA_001` exists for live POM. Logs: `tmp/dev-stack/`. Stop host apps with `scripts/dev-stack.sh --stop`. Tear down Compose with `scripts/dev-stack.sh --down`.

Canonical Playwright POM origin on this stack is `http://localhost:3000` (`playwright.pom.config.ts` uses `reuseExistingServer: true`). HTTP Nuxt binds `127.0.0.1`. The TLS overlay binds `0.0.0.0` so Caddy can reach the host — see [tls-lab.md](tls-lab.md).

HTTPS overlay: [tls-lab.md](tls-lab.md).


From the repository root:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

## Service URLs

- PostgreSQL: `localhost:5432` by default
- Keycloak: `http://localhost:8081` by default

## Readiness Checks

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml ps
```

PostgreSQL readiness is checked with `pg_isready`. Keycloak starts in development mode and may take longer than PostgreSQL.

## Stop Services

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

Use this only when you intentionally want to remove the local database volume:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down -v
```

## Troubleshooting

- If ports conflict, change `POSTGRES_PORT` or `KEYCLOAK_PORT` in `infra/compose/.env`.
- PostgreSQL 18 stores data below a major-version-specific directory, so the Compose volume is mounted at `/var/lib/postgresql` rather than `/var/lib/postgresql/data`.
- If Keycloak takes time to become ready, inspect logs with `docker compose --env-file infra/compose/.env -f infra/compose/compose.yml logs payment-quality-keycloak`.
- Do not confuse Keycloak infrastructure readiness with application auth integration. Phase 0 does not wire backend or frontend auth flows.

## Non-Production Warning

These credentials and startup options are for local development only. They are not production security guidance.

## See Also

- [Root README](../../README.md) — project scope, non-goals, and full baseline verification commands
- [Tester Orientation Pack](phase-0-tester-orientation-pack.md) — what exists, what is absent, and tester charters
- Phase 0 non-goals reminder: no payment business functionality, no Kafka, no PSP integration, no complete OAuth/OIDC application integration, no complete dashboards
