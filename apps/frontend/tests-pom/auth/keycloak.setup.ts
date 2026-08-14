import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { expect, type Page } from '@playwright/test'
import type { PomAccount } from './accounts'
import { LoginPage } from '../pages/LoginPage'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

export async function saveKeycloakStorageState(page: Page, account: PomAccount, path: string): Promise<void> {
  mkdirSync(dirname(path), { recursive: true })
  const login = new LoginPage(page)
  await login.goto()
  await login.expectLoaded()
  await page.goto('/auth/keycloak')
  await page.getByLabel('Username or email').fill(account.username)
  await page.getByRole('textbox', { name: 'Password' }).fill(account.password)
  await page.getByRole('button', { name: /sign in/i }).click()

  const updateProfile = page.getByRole('heading', { name: /update account information/i })
  if (await updateProfile.isVisible({ timeout: 3_000 }).catch(() => false)) {
    throw new Error('Keycloak user requires an interactive profile update; use the imported realm users.')
  }

  await page.waitForURL('**/admin/merchants', { timeout: 30_000 })
  const session = await page.evaluate(async () => {
    const response = await fetch('/api/_auth/session')
    if (!response.ok) {
      throw new Error(`sealed Nuxt session must exist after Keycloak login (${response.status})`)
    }
    return await response.json() as { user?: { roles?: string[], tenantId?: string, merchantId?: string } }
  })
  expect(session.user?.roles).toContain(account.role)
  expect(session.user?.tenantId).toBe(account.tenantId)
  expect(session.user?.merchantId).toBe(account.merchantId)
  await expectNoTokenInBrowserStorage(page)
  await page.context().storageState({ path })
}
