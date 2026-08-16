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

**How to run (HTTP POM, HTTPS/Caddy, TypeScript):** [run-stack-and-pom.md](run-stack-and-pom.md).

## Stack modes (Podman)

Postgres and Keycloak always run in Compose. Spring and Nuxt are either host processes (hot reload) or images.

```bash
scripts/dev-stack.sh            # HTTP DX: host Spring + Nuxt
scripts/dev-stack.sh --app      # HTTP deploy: Spring + Nuxt containers (POM on :3000)
scripts/dev-stack.sh --tls      # HTTPS overlay: Caddy → host processes
scripts/dev-stack.sh --full     # HTTPS deploy: Caddy → Spring + Nuxt containers
```

`--app` and `--full` require `NUXT_SESSION_PASSWORD` (≥32) in `infra/compose/.env` (Nitro production image). `--tls` / `--full` also need `scripts/tls-lab-certs.sh`. See [tls-lab.md](tls-lab.md).

The script waits for Postgres, then **Keycloak OIDC issuer** (`scripts/keycloak-issuer-oracle.sh`). HTTP modes must advertise `http://localhost:8081`. HTTPS modes must advertise `https://auth.payment-quality.local:8443`. A leftover container from the other mode is recreated automatically. `up` always uses `--remove-orphans` so Caddy/app containers from a previous mode do not keep port `:3000`.

Rootless Podman on Fedora uses pasta. `podman-compose` sometimes records published ports without starting `rootlessport`. `dev-stack.sh --app` / `--full` stop/start the app (and Caddy) containers after they are healthy so `:3000`, `:8080`, and `:8443` actually bind on the host.

Canonical Playwright POM origin on HTTP is `http://127.0.0.1:3000` (`PLAYWRIGHT_SKIP_WEBSERVER=1` when using `--app`). HTTP host Nuxt binds `127.0.0.1`. The TLS overlay binds `0.0.0.0` so Caddy can reach the host — see [tls-lab.md](tls-lab.md).

Stop host apps with `scripts/dev-stack.sh --stop`. Tear down Compose with `scripts/dev-stack.sh --down`.

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

- [run-stack-and-pom.md](run-stack-and-pom.md) — daily HTTP `--app`, HTTPS `--full`, POM, Caddy FAQ
- [bruno-postman-api.md](bruno-postman-api.md) — Bearer JWT via Keycloak ROPC in Bruno/Postman
- [Tester Orientation Pack](phase-0-tester-orientation-pack.md) — what exists, what is absent, and tester charters
- Phase 0 non-goals reminder: no payment business functionality, no Kafka, no PSP integration, no complete OAuth/OIDC application integration, no complete dashboards
