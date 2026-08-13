# Learner POM tree

Copy TypeScript from [`../tests-pom`](../tests-pom). Keep the same folders: `auth/`, `fixtures/`, `pages/`, `api/`, `data/`, `utils/`, `specs/`.

**Copy `auth/` first** (including `platform-admin.setup.ts`, `merchant-manager.setup.ts`, `keycloak.setup.ts`, `accounts.ts`) plus `pages/LoginPage.ts` and `utils/`. The learner config has the same setup projects as the reference tree, plus `chromium-guest` (empty `storageState`, no Keycloak setup). Guest specs must not use the `api` fixture. Auth files write to `tests-pom-learner/.auth/` (gitignored). Specs without that setup will fail before any page object runs.

Name your files `My*` (for example `pages/MyMerchantsListPage.ts`, `specs/My.merchants.spec.ts`).

Do not import page objects from `tests-pom` — the point is to type them yourself.

```bash
cd apps/frontend
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
corepack pnpm exec playwright test --config playwright.pom-learner.config.ts
```

Preflight (Compose + Spring `dev`) is in [`../tests-pom/README.md`](../tests-pom/README.md).
