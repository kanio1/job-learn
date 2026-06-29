/**
 * F-A2: Role session helpers for multi-role Playwright tests.
 *
 * Provides typed session mocks for each composite role. The session is injected
 * at the browser level via page.route() — no real JWT is generated, no token is
 * committed, no Keycloak call is made (unless PLAYWRIGHT_USE_REAL_KEYCLOAK=true).
 *
 * The `/api/_auth/session` endpoint is the Nuxt sealed-session reader used by:
 *   - auth.global.ts middleware (redirect unauthenticated users)
 *   - useUserSession() (exposes `user` to composables)
 *   - useAuthorization() (derives capability booleans from user.roles)
 *
 * Supported roles (2 required + 1 optional by Phase 3A-4 spec):
 *   PLATFORM_ADMIN  — full platform access (read/create/update merchants, payments, users)
 *   MERCHANT_MANAGER — payment create + lifecycle for own merchant
 *   SUPPORT_AGENT  — read-only across merchants and payments, view audit log
 *
 * RBAC capabilities per role are the authoritative source in ~/utils/rbacMatrix.ts.
 *
 * Token safety: roles are resolved server-side by OIDC. The `user` object
 * exposed to the browser contains NO tokens, NO Authorization header,
 * NO JWT claims beyond the listed roles array.
 */

import type { Page } from '@playwright/test'

export type MockRoleName = 'PLATFORM_ADMIN' | 'MERCHANT_MANAGER' | 'SUPPORT_AGENT'

interface MockSession {
  loggedIn: true
  user: { username: string; roles: MockRoleName[] }
}

const SESSION_BY_ROLE: Record<MockRoleName, MockSession> = {
  PLATFORM_ADMIN: {
    loggedIn: true,
    user: { username: 'platform.operator', roles: ['PLATFORM_ADMIN'] },
  },
  MERCHANT_MANAGER: {
    loggedIn: true,
    user: { username: 'merchant.manager', roles: ['MERCHANT_MANAGER'] },
  },
  SUPPORT_AGENT: {
    loggedIn: true,
    user: { username: 'support.agent', roles: ['SUPPORT_AGENT'] },
  },
}

/**
 * Register a page.route() mock for /api/_auth/session that returns the given role.
 * Must be called BEFORE page.goto() so the mock is active on first navigation.
 *
 * Uses LIFO route ordering: call this after any broader session mocks to ensure
 * this handler takes precedence.
 */
export async function mockRoleSession(page: Page, role: MockRoleName): Promise<void> {
  await page.route('**/api/_auth/session', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(SESSION_BY_ROLE[role]),
    }),
  )
}

/** Session data for inline use (e.g. in route.fulfill body) without the page.route wrapper. */
export function sessionDataForRole(role: MockRoleName): MockSession {
  return SESSION_BY_ROLE[role]
}
