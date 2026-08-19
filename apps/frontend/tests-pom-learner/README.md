# Learner POM tree

Active lesson: [`LESSON-08-first-real-pom.md`](./LESSON-08-first-real-pom.md) — first real POM, three accounts, RBAC vs tenant scope. Do not copy business page objects from `tests-pom`.

Copy TypeScript from [`../tests-pom`](../tests-pom). Keep the same folders: `auth/`, `fixtures/`, `pages/`, `api/`, `data/`, `utils/`, `specs/`.

**Copy `auth/` first** (including `platform-admin.setup.ts`, `merchant-manager.setup.ts`, `keycloak.setup.ts`, `accounts.ts`) plus `pages/LoginPage.ts` and `utils/`. The learner config has the same setup projects as the reference tree, plus `chromium-guest` (empty `storageState`, no Keycloak setup). Guest specs must not use the `api` fixture. Auth files write to `tests-pom-learner/.auth/` (gitignored). Specs without that setup will fail before any page object runs.

Name your files `My*` (for example `pages/MyMerchantsListPage.ts`, `specs/My.merchants.spec.ts`).

Do not import page objects from `tests-pom` — the point is to type them yourself.

```bash
cd apps/frontend
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_TENANT_ADMIN_PASSWORD=tenant.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
export PLAYWRIGHT_SUPPORT_AGENT_PASSWORD=support.agent
export PLAYWRIGHT_READ_ONLY_PASSWORD=readonly.user
PLAYWRIGHT_SKIP_WEBSERVER=1 PLAYWRIGHT_BASE_URL=http://localhost:3000 \
  corepack pnpm exec playwright test --config playwright.pom-learner.config.ts
```

Against `scripts/dev-stack.sh --app`, `PLAYWRIGHT_SKIP_WEBSERVER=1` is required (Playwright must not start host `pnpm dev`). Browser origin is `http://localhost:3000`, not `127.0.0.1`. Preflight: [`../tests-pom/README.md`](../tests-pom/README.md).
