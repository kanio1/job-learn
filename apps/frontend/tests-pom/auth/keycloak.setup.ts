import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { expect, type Page } from '@playwright/test'
import '../utils/ipv4-first'
import type { PomAccount } from './accounts'
import { LoginPage } from '../pages/LoginPage'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

export async function saveKeycloakStorageState(page: Page, account: PomAccount, path: string): Promise<void> {
  mkdirSync(dirname(path), { recursive: true })
  await completeKeycloakLogin(page, account.username, account.password)
  // Fresh logins under parallel setup projects can take >30s (Keycloak + Nuxt OIDC
  // round-trip during stack warm-up); allow a generous window.
  await page.waitForURL('**/admin/merchants', { timeout: 120_000 })
  const session = await page.evaluate(async () => {
    const response = await fetch('/api/_auth/session')
    if (!response.ok) {
      throw new Error(`sealed Nuxt session must exist after Keycloak login (${response.status})`)
    }
    return await response.json() as { user?: { roles?: string[], tenantId?: string, merchantId?: string } }
  })
  expect(session.user?.roles).toContain(account.role)
  expect(session.user?.tenantId).toBe(account.tenantId)
  expect(session.user?.merchantId ?? undefined).toBe(account.merchantId)
  await expectNoTokenInBrowserStorage(page)
  await page.context().storageState({ path })
}

async function completeKeycloakLogin(page: Page, username: string, password: string): Promise<void> {
  const login = new LoginPage(page)
  await login.goto()
  await login.expectLoaded()
  await page.goto('/auth/keycloak')
  await page.getByLabel('Username or email').fill(username)
  await page.getByRole('textbox', { name: 'Password' }).fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()

  const updateProfile = page.getByRole('heading', { name: /update account information/i })
  if (await updateProfile.isVisible({ timeout: 3_000 }).catch(() => false)) {
    throw new Error('Keycloak user requires an interactive profile update; use the imported realm users.')
  }
}

export async function saveDeniedStorageState(
  page: Page,
  account: { username: string, password: string },
  path: string,
): Promise<void> {
  mkdirSync(dirname(path), { recursive: true })
  await completeKeycloakLogin(page, account.username, account.password)
  await page.waitForURL((url) => {
    const href = url.href
    return !href.includes('/login') && !href.includes('/realms/') && !href.includes('/auth/keycloak')
  }, { timeout: 120_000 })

  const session = await page.evaluate(async () => {
    const response = await fetch('/api/_auth/session')
    if (!response.ok) {
      throw new Error(`sealed Nuxt session must exist after Keycloak login (${response.status})`)
    }
    return await response.json() as { user?: { roles?: string[] } }
  })
  expect(session.user, 'denied user must have a Nuxt session').toBeTruthy()
  const roles = session.user?.roles ?? []
  expect(roles.some(role => role.startsWith('platform:') || role.startsWith('merchant:'))).toBe(false)
  expect(roles).not.toContain('PLATFORM_ADMIN')
  expect(roles).not.toContain('MERCHANT_MANAGER')
  await expectNoTokenInBrowserStorage(page)
  await page.context().storageState({ path })
}
