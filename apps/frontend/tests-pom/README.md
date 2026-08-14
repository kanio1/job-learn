# Live POM framework (reference)

Playwright **Page Object Model** on the real stack: Keycloak + Nuxt BFF + Spring + Postgres.

This tree is the **agent reference**. Write your own copy in [`../tests-pom-learner`](../tests-pom-learner). Do not import from `tests/e2e` (those specs mock the BFF).

## Rules

- **No** `page.route` / `route.fulfill` in `tests-pom` (live suite). Mocked interception, HAR, and visual goldens live in `tests/e2e` (`testMatch` there). `@visual-negative` is excluded from default Chromium via `grepInvert`.
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
```

Optional usernames: `PLAYWRIGHT_PLATFORM_ADMIN_USERNAME`, `PLAYWRIGHT_MERCHANT_MANAGER_USERNAME`.

## Run

From `apps/frontend`:

```bash
corepack pnpm exec playwright test --config playwright.pom.config.ts
```

Learner tree (starts with zero tests):

```bash
corepack pnpm exec playwright test --config playwright.pom-learner.config.ts
```

TLS overlay (`playwright.pom.tls.config.ts`): `scripts/dev-stack.sh --tls` or `--full`, then the same password exports. On `--full` set `PLAYWRIGHT_SKIP_WEBSERVER=1`. Chromium trusts mkcert after `mkcert -install` (Linux Chromium may need `nss-tools` / `certutil`). Without OS trust, `PLAYWRIGHT_TLS_INSECURE=1` — that does not prove CA trust. ConfirmModal dismiss uses `data-testid="confirm-action-dismiss"` (never the drawer button labelled Cancel). A `vite-plugin-checker-error-overlay` fails the POM (`count() > 0`); live `webServer` uses `NUXT_TYPECHECK=false`.

FE on + Spring RLS off (`playwright.pom.rls-spring-off.config.ts`): `RLS_LAB_ENABLED=false` on Spring and `PLAYWRIGHT_RLS_SPRING_OFF=1`.

## Layout (classic skeleton)

```text
auth/        storageState setup (real OIDC)
fixtures/    test.extend — App facade + BffClient
pages/       POM (BasePage, components, one class per screen)
api/         app-as-API against the BFF
data/        unique factories
utils/       env + problem+json
specs/       flows only
```

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
| `fixtures/index.ts` | `fixtures/index.ts` |
| …same folder names… | copy then rename |

Patterns: POM (intent methods), fixture DI, `App` facade, data factory, API client for preconditions. Assertions stay in specs except `expectLoaded()`.
