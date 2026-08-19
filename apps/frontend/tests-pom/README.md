# Live POM framework (reference)

Playwright **Page Object Model** on the real stack: Keycloak + Nuxt BFF + Spring + Postgres.

This tree is the **only product Playwright suite** (UI/E2E + BFF REST). Write a learner copy in [`../tests-pom-learner`](../tests-pom-learner). Vitest stays in `../tests/unit`.

## Rules

- **No** `page.route` / `route.fulfill` in `tests-pom`. Visual goldens and ARIA snapshots live here under `PLAYWRIGHT_VISUAL=1`. `@visual-negative` is excluded from default Chromium via `grepInvert`.
- Passwords **only** via environment variables.
- Do not commit `tests-pom/.auth/*.json`.
- Error Lab **429** is a BFF mock — this suite does not call it.
- Hosted checkout is a **new tab**, not an iframe.
- Assert Idempotency-Key / If-Match / ETag with `page.waitForRequest` (never mocks).
- Guest project (`chromium-guest`) uses empty `storageState` — do not destructure `api`.

## Tags

`@security` session/IDOR/412, `@a11y` ARIA snapshot, `@ux` palette/notes/risk/checkout.

Test map (E2E, HTTP/BFF, security, EP/BVA, DT/UC): [`docs/testing/live-pom-wave-2/`](../../../docs/testing/live-pom-wave-2/README.md). Sibling catalogs: checkout-protocol-lab, payu-bank-mirror-labs, rls-filters-composition-lab.

## Containers

| Layer | How |
|---|---|
| Postgres + Keycloak | **Podman/Docker Compose** — [`infra/compose/compose.yml`](../../../infra/compose/compose.yml) |
| Spring Boot | host `dev,seed` **or** `--app` / `--full` image |
| Nuxt | Playwright `webServer` / host `:3000` **or** `--app` / `--full` image |
| Caddy | `--tls` (host apps) or `--full` (container apps). **Not** started by `--app`. |

Runbook: [`docs/setup/run-stack-and-pom.md`](../../../docs/setup/run-stack-and-pom.md).

## Preflight

From the repository root:

```bash
cp infra/compose/.env.example infra/compose/.env   # once
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

Backend (`apps/backend`):

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Passwords (realm defaults match usernames; still do not commit them):

```bash
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
export PLAYWRIGHT_TENANT_ADMIN_PASSWORD=tenant.admin
export PLAYWRIGHT_SUPPORT_AGENT_PASSWORD=support.agent
export PLAYWRIGHT_READ_ONLY_PASSWORD=readonly.user
# optional until Fala 2 worker setup; realm defaults match usernames
export PLAYWRIGHT_MERCHANT_MANAGER_W0_PASSWORD=merchant.manager.w0
export PLAYWRIGHT_MERCHANT_MANAGER_W1_PASSWORD=merchant.manager.w1
export PLAYWRIGHT_MERCHANT_MANAGER_W2_PASSWORD=merchant.manager.w2
export PLAYWRIGHT_MERCHANT_MANAGER_W3_PASSWORD=merchant.manager.w3
```

Optional usernames: `PLAYWRIGHT_PLATFORM_ADMIN_USERNAME`, `PLAYWRIGHT_MERCHANT_MANAGER_USERNAME`, `PLAYWRIGHT_TENANT_ADMIN_USERNAME`, `PLAYWRIGHT_SUPPORT_AGENT_USERNAME`, `PLAYWRIGHT_MERCHANT_MANAGER_W{0-3}_USERNAME`.

Worker world (seed + realm; empty payment lists):

| Worker | User | Merchant UUID | Reference |
|---|---|---|---|
| 0 | `merchant.manager.w0` | `…000d0` | `MERCHANT-W0` |
| 1 | `merchant.manager.w1` | `…000d1` | `MERCHANT-W1` |
| 2 | `merchant.manager.w2` | `…000d2` | `MERCHANT-W2` |
| 3 | `merchant.manager.w3` | `…000d3` | `MERCHANT-W3` |

`merchant.manager` stays on Alpha (`MERCHANT_ALPHA_001`) for serial ISO. Contract seed (~104 orders) is unchanged.

Keycloak `--import-realm` does **not** update an existing realm. After this seed/realm change either recreate the Keycloak volume or run `python3 scripts/provision-pom-worker-keycloak-users.py` (declares `tenant_id` / `merchant_id` on the user profile, then upserts `merchant.manager.w0`–`w3`). Spring `dev,seed` re-seeds merchants on startup.

Do **not** call `POST /api/test/seed-learning` or `/api/test/etl/payments/*` from this suite. Contract world is `dev,seed` (~104) plus unique factories.

## Run

Against HTTP compose (`scripts/dev-stack.sh --app`, no Caddy):

```bash
# from repo root
scripts/run-app-stack-tests.sh
scripts/run-app-stack-tests.sh --visual     # Visual Lab + ARIA goldens
scripts/run-app-stack-tests.sh --rls-off    # second Nuxt :3010, flag off
```

Worker isolation proof (`chromium-worker`, 2 workers, empty `MERCHANT-Wn`):

```bash
corepack pnpm exec playwright test --config playwright.pom.config.ts --project=chromium-worker
```

Or from `apps/frontend` (`pnpm test:e2e` is the live POM; it does not start mocked Chromium):

```bash
corepack pnpm test:e2e
```

Learner tree (starts with zero tests):

```bash
corepack pnpm exec playwright test --config playwright.pom-learner.config.ts
```

TLS overlay (`playwright.pom.tls.config.ts`): `scripts/dev-stack.sh --tls` or `--full`, then the same password exports. On `--full` set `PLAYWRIGHT_SKIP_WEBSERVER=1`. Chromium trusts mkcert after `mkcert -install` (Linux Chromium may need `nss-tools` / `certutil`). Without OS trust, `PLAYWRIGHT_TLS_INSECURE=1` — that does not prove CA trust. ConfirmModal dismiss uses `data-testid="confirm-action-dismiss"` (never the drawer button labelled Cancel). A `vite-plugin-checker-error-overlay` fails the POM (`BasePage` throws if `count() > 0`); fixtures do not dismiss it. Live `webServer` uses `NUXT_TYPECHECK=false`.

REST 4xx on BFF uses `expectProblem` (`status` + title/detail; `error` when the contract names it). Merchant reactivate 409 is `ErrorResponse.error=invalid_transition`, not problem+json.

`chromium-manager` logs `merchant.manager.w{n}` and mutates `MERCHANT-Wn` (`fullyParallel`). Default workers: **2 on CI, 4 locally**, capped at 4 (`PLAYWRIGHT_WORKERS` overrides). Do not use Playwright’s CPU default.

Browser `PLAYWRIGHT_BASE_URL` must be `http://localhost:3000` (OIDC `redirectURL`). Node `BffClient` rewrites that host to `127.0.0.1`. Do not point Playwright at `127.0.0.1` — the state cookie will not match the Keycloak callback.

- `setup-worker-managers` — logs `w0`–`w3` once (no lazy `existsSync` race).
- `chromium-serial` — tenant settings + dual-control (project default is platform-admin; specs may `test.use` manager).
- `chromium-session` — own `platform-admin-session.json` so logout/revoke cannot skip or poison admin.
- `chromium-rbac` / `chromium-visual` — `fullyParallel: false` (seed-world / goldens).

FE on + Spring RLS off (`playwright.pom.rls-spring-off.config.ts`): `RLS_LAB_ENABLED=false` on Spring and `PLAYWRIGHT_RLS_SPRING_OFF=1`.

## Layout (classic skeleton)

```text
auth/        storageState setup (real OIDC) — admin, tenant.admin, manager
fixtures/    test.extend — App facade + BffClient + workerWorld
pages/       POM (BasePage, components, one class per screen)
api/         app-as-API against the BFF (no seed-learning / ETL)
data/        unique factories + PaymentOrderDraft
utils/       env, http, wait-bff, roles.openAs, dates, problem, persistence
methods/     ISTQB rows + combinations (see methods/README.md)
specs/       flows only
```

Method playbook: [`docs/testing/playwright-method-playbook/`](../../../docs/testing/playwright-method-playbook/README.md).

## Copy map (reference → your My* files)

| Reference | Learner |
|---|---|
| `pages/MerchantsListPage.ts` | `pages/MyMerchantsListPage.ts` |
| `pages/SupportPage.ts` | `pages/MySupportPage.ts` |
| `pages/components/UserMenu.ts` | `pages/components/MyUserMenu.ts` |
| `pages/App.ts` | `pages/MyApp.ts` |
| `specs/merchants.spec.ts` | `specs/My.merchants.spec.ts` |
| `specs/session-guest.spec.ts` | `specs/My.session-guest.spec.ts` |
| `specs/session.spec.ts` | `specs/My.session.spec.ts` |
| `specs/command-palette.spec.ts` | `specs/My.command-palette.spec.ts` |
| `specs/internal-notes.spec.ts` | `specs/My.internal-notes.spec.ts` |
| `specs/merchant-risk.spec.ts` | `specs/My.merchant-risk.spec.ts` |
| `specs/support.spec.ts` | `specs/My.support.spec.ts` |
| `specs/payments-create.spec.ts` | `specs/My.payments-create.spec.ts` |
| `specs/auth-rbac.spec.ts` | `specs/My.auth-rbac.spec.ts` |
| `specs/tenant-scope.spec.ts` | `specs/My.tenant-scope.spec.ts` |
| `auth/tenant-admin.setup.ts` | `auth/tenant-admin.setup.ts` |
| `methods/combinations/IsolationDtUc.ts` | `methods/combinations/MyIsolationDtUc.ts` |
| `utils/wait-bff.ts` | `utils/wait-bff.ts` |
| `fixtures/index.ts` | `fixtures/index.ts` |
| …same folder names… | copy then rename |

Patterns: POM (intent methods), fixture DI, `App` facade, data factory, API client for preconditions. Assertions stay in specs except `expectLoaded()`. Agent placement: `.agents/skills/playwright-pom`.
