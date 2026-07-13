import { expect, type Page } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { expectNoTokenInBrowserStorage } from '../../support/browser-safety-assertions'

export const liveAuthFiles = {
  platformAdmin: 'tests/.auth/live-platform-admin.json',
  merchantManager: 'tests/.auth/live-merchant-manager.json',
} as const

type LiveRole = 'PLATFORM_ADMIN' | 'MERCHANT_MANAGER'

interface LiveAccount {
  username: string
  password: string
  role: LiveRole
  tenantId: string
  merchantId?: string
}

function requiredEnvironmentValue(name: string): string {
  const value = process.env[name]
  if (!value) {
    throw new Error(`Live Playwright setup requires ${name}; credentials are supplied only through the environment.`)
  }
  return value
}

export function platformAdminAccount(): LiveAccount {
  return {
    username: process.env.PLAYWRIGHT_PLATFORM_ADMIN_USERNAME || 'platform.admin',
    password: requiredEnvironmentValue('PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD'),
    role: 'PLATFORM_ADMIN',
    tenantId: 'PLATFORM_TENANT',
  }
}

export function merchantManagerAccount(): LiveAccount {
  return {
    username: process.env.PLAYWRIGHT_MERCHANT_MANAGER_USERNAME || 'merchant.manager',
    password: requiredEnvironmentValue('PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD'),
    role: 'MERCHANT_MANAGER',
    tenantId: 'TENANT_ALPHA',
    merchantId: 'MERCHANT_ALPHA_001',
  }
}

export async function saveLiveKeycloakStorageState(
  page: Page,
  account: LiveAccount,
  storageStatePath: string,
): Promise<void> {
  mkdirSync(dirname(storageStatePath), { recursive: true })

  await page.goto('/login')
  await expect(page.getByTestId('login-control')).toBeVisible()
  // Navigate through the actual Nuxt OIDC handler. Direct navigation is used
  // because a client-side external navigation does not reliably await a
  // cross-origin redirect in headless setup projects.
  await page.goto('/auth/keycloak')
  await page.getByLabel('Username or email').fill(account.username)
  await page.getByRole('textbox', { name: 'Password' }).fill(account.password)
  await page.getByRole('button', { name: /sign in/i }).click()

  const updateProfile = page.getByRole('heading', { name: /update account information/i })
  if (await updateProfile.isVisible({ timeout: 3_000 }).catch(() => false)) {
    throw new Error('Live Keycloak user requires an interactive profile update; deterministic test users must be preconfigured.')
  }

  await page.waitForURL('**/admin/merchants', { timeout: 30_000 })
  const sessionResponse = await page.request.get('/api/_auth/session')
  expect(sessionResponse.ok(), 'the sealed Nuxt session must exist after real Keycloak login').toBe(true)
  const session = await sessionResponse.json() as {
    user?: { roles?: string[], tenantId?: string, merchantId?: string }
  }

  expect(session.user).toBeDefined()
  expect(session.user?.roles).toContain(account.role)
  expect(session.user?.tenantId).toBe(account.tenantId)
  expect(session.user?.merchantId).toBe(account.merchantId)
  await expectNoTokenInBrowserStorage(page)
  await page.context().storageState({ path: storageStatePath })
}
