# Playwright Auth and Data Isolation Strategy

> Implemented in Phase 3A-4 (F-A2, F-A4). Applies to `apps/frontend/tests/`.

---

## Overview

All Playwright tests in this project use **mock-based authentication**: the Nuxt sealed-session endpoint (`/api/_auth/session`) is intercepted at the browser level via `page.route()`. No real Keycloak interaction is required in CI.

Real Keycloak login is available but opt-in (`PLAYWRIGHT_USE_REAL_KEYCLOAK=true`). Storage state files (`.auth/*.json`) are placeholders (`{ cookies: [], origins: [] }`) in version control.

---

## Roles Supported

| Role | Implemented | Storage State File |
|---|---|---|
| `PLATFORM_ADMIN` | ✅ Phase 3A-4 | `tests/.auth/platform-operator.json` (placeholder) |
| `MERCHANT_MANAGER` | ✅ Phase 3A-4 | `tests/.auth/merchant-manager.json` (placeholder) |
| `SUPPORT_AGENT` | ✅ Phase 3A-4 | — (mock session only, no separate storage state) |

The authoritative capability matrix is in `app/utils/rbacMatrix.ts`. Key differences:

| Capability | PLATFORM_ADMIN | SUPPORT_AGENT | MERCHANT_MANAGER |
|---|---|---|---|
| `canCreateMerchant` | ✅ | ✗ | ✗ |
| `canReadMerchants` | ✅ | ✅ | ✗ |
| `canUpdateMerchantStatus` | ✅ | ✗ | ✗ |
| `canCreatePaymentOrder` | ✅ | ✗ | ✅ |
| `canReadMerchantPayments` | ✗ | ✗ | ✅ |
| `canRunLifecycle` | ✅ | ✗ | ✅ |
| `canViewAuditLog` | ✅ | ✅ | ✗ |
| `canManageUsers` | ✅ | ✗ | ✗ |

---

## Auth Strategy: Option A (Mock Session)

### How it works

```typescript
// tests/support/auth-roles.ts
import { mockRoleSession } from '../../support/auth-roles'

// In a test:
await mockRoleSession(page, 'PLATFORM_ADMIN')    // sets roles: ['PLATFORM_ADMIN']
await mockRoleSession(page, 'SUPPORT_AGENT')     // sets roles: ['SUPPORT_AGENT']
await mockRoleSession(page, 'MERCHANT_MANAGER')  // sets roles: ['MERCHANT_MANAGER']
```

`mockRoleSession()` registers a `page.route('**/api/_auth/session', ...)` handler that returns a `{ loggedIn: true, user: { username, roles } }` JSON response. The `useAuthorization()` composable reads `user.roles` and derives `can.*` capability booleans from `rbacMatrix`.

### Why mock-based

1. All existing tests already use this pattern (see `merchant-support.ts`, `confirm-action-modal.spec.ts`).
2. No Keycloak dependency in CI.
3. Role switching within a single test file is trivial — different tests call `mockRoleSession` with different roles.
4. Zero token risk: no JWT is generated, no `Authorization` header is present, no `eyJ` bytes in test data.

### Real Keycloak opt-in

Set `PLAYWRIGHT_USE_REAL_KEYCLOAK=true` to run the `auth-setup` and `merchant-manager-auth-setup` projects with live Keycloak login. The resulting browser sessions are saved to `.auth/*.json` (git-ignored). This path is only needed for full end-to-end integration testing with a running Keycloak instance.

---

## Playwright Projects (playwright.config.ts)

```
api-tests                     — APIRequestContext tests, no browser, no auth
auth-setup                    — writes platform-operator.json placeholder
merchant-manager-auth-setup   — writes merchant-manager.json placeholder
chromium                      — all e2e/**/*.spec.ts, platform-operator storageState
chromium-merchant-manager     — e2e/merchant-manager/**/*.spec.ts (skeleton, 0 tests currently)
```

### Adding a new merchant-manager test

1. Create the test in `tests/e2e/merchant-manager/`.
2. Call `await mockRoleSession(page, 'MERCHANT_MANAGER')` at the top of each test.
3. The `chromium-merchant-manager` project will automatically pick it up.

---

## Comparison Test Pattern (F-A2)

Use `test.describe` blocks to group role-visibility comparisons in a single file:

```typescript
// tests/e2e/rbac/role-visibility.spec.ts
test.describe('canCreateMerchant gating', () => {
  test('PLATFORM_ADMIN sees create merchant button', async ({ page }) => {
    await mockRoleSession(page, 'PLATFORM_ADMIN')
    await mockMerchantsListApi(page)
    await page.goto('/admin/merchants')
    await expect(page.getByRole('button', { name: /Search/ })).toBeVisible({ timeout: 15000 })
    await expect(page.getByTestId('action-create-merchant')).toBeVisible()
  })

  test('SUPPORT_AGENT does not see create merchant button', async ({ page }) => {
    await mockRoleSession(page, 'SUPPORT_AGENT')
    await mockMerchantsListApi(page)
    await page.goto('/admin/merchants')
    await expect(page.getByRole('button', { name: /Search/ })).toBeVisible({ timeout: 15000 })
    await expect(page.getByTestId('action-create-merchant')).not.toBeVisible()
  })
})
```

Key rules for comparison tests:
- **Always wait for a positive anchor before asserting absence.** `expect(X).not.toBeVisible()` passes immediately if the element isn't in the DOM — including during page load. Wait for a stable element first.
- **Mock API endpoints alongside the session** to prevent the page from hitting the real backend.
- **Merchant status must match the Zod schema**: `z.enum(['PENDING', 'ACTIVE', 'SUSPENDED'])`. Do not use `'DRAFT'`.
- **Use `data-testid` selectors** for RBAC-gated elements. These are stable contracts, not CSS implementation details.

---

## Data Isolation Strategy (F-A4)

### Current status

`fullyParallel` is `false` in `playwright.config.ts`. All tests run in a single worker (worker index 0). No test data isolation is currently needed for correctness.

### Worker-aware naming (ready for parallel future)

```typescript
// tests/support/test-data-isolation.ts
import { isolatedMerchantRef, isolatedPaymentRef } from '../support/test-data-isolation'

// In a test that uses testInfo:
const merchantRef = isolatedMerchantRef(testInfo.workerIndex, 'CREATE')
// → "TEST-W0-CREATE" on worker 0, "TEST-W1-CREATE" on worker 1

const paymentRef = isolatedPaymentRef(testInfo.workerIndex, 'LIFECYCLE')
// → "PAY-W0-LIFECYCLE" on worker 0
```

All tests that CREATE or MUTATE real backend records must use these prefixes when `fullyParallel` is enabled.

### When to enable fullyParallel

Prerequisites — all four must hold:

1. **Every mutation test uses worker-prefixed identifiers** (this module provides them).
2. **A global `beforeAll` resets AND seeds per-worker data** using `resetTestData()` / `seedTestData()` from `test-data-isolation.ts`.
3. **No test reads records created by another worker.**
4. **The backend seed/reset API is available** — see next section.

Set `fullyParallel: true` in `playwright.config.ts` once all four hold.

---

## Seed/Reset API (BLOCKED in CI)

The Spring backend provides test lifecycle endpoints:

```
POST /api/test/reset  — clears all tenants/merchants/payments
POST /api/test/seed   — clears and seeds a deterministic dataset (see Fixtures.java)
```

### Why blocked

1. **Backend not started**: the Playwright webServer in `playwright.config.ts` starts only the Nuxt dev server (port 3000). The Spring backend (port 8080) is not started as part of `pnpm exec playwright test`.
2. **Disabled by default**: `app.testing.enabled=false` in `application.yml`. The endpoints are gated by `@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")` and `@Profile("!prod")`.
3. **No BFF proxy**: there are no `server/api/test/**` routes. Playwright must call the backend directly via `PLAYWRIGHT_BACKEND_URL`.

### How to unblock (when needed)

```bash
# 1. Start backend with testing endpoints enabled
export SPRING_PROFILES_ACTIVE=dev
export APP_TESTING_ENABLED=true   # or set in application-dev.yml
cd apps/backend && ./mvnw spring-boot:run

# 2. Point Playwright at the backend
export PLAYWRIGHT_BACKEND_URL=http://localhost:8080

# 3. Run tests that use reset/seed
corepack pnpm exec playwright test tests/api/
```

The `resetTestData()` and `seedTestData()` helper functions are implemented and ready in `tests/support/test-data-isolation.ts`.

---

## File Inventory (Phase 3A-4)

| File | Purpose |
|---|---|
| `tests/support/auth-roles.ts` | Typed role session helper (`mockRoleSession`) |
| `tests/support/test-data-isolation.ts` | Worker naming convention + seed/reset API helpers |
| `tests/auth/merchant-manager.setup.ts` | Merchant-manager storage state setup project |
| `tests/e2e/rbac/role-visibility.spec.ts` | 4 RBAC comparison tests (F-A2) |
| `tests/api/data-isolation-naming.spec.ts` | 5 naming convention validation tests (F-A4) |
| `playwright.config.ts` | Added `merchant-manager-auth-setup` + `chromium-merchant-manager` projects |
