/**
 * F-A2: Merchant manager storage state setup.
 *
 * Mirrors auth.setup.ts for the MERCHANT_MANAGER role.
 *
 * Default mode (no PLAYWRIGHT_USE_REAL_KEYCLOAK): writes a placeholder
 * storageState with empty cookies. The actual role session is injected
 * per-test via mockRoleSession() from tests/support/auth-roles.ts.
 *
 * Real Keycloak mode (PLAYWRIGHT_USE_REAL_KEYCLOAK=true): performs a
 * live login with the merchant.manager account and saves the resulting
 * browser session to .auth/merchant-manager.json.
 *
 * The .auth/ directory is excluded from version control. No real tokens
 * are committed. The placeholder JSON { cookies: [], origins: [] } is safe.
 */

import { test as setup } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'node:fs'

const authFile = 'tests/.auth/merchant-manager.json'

setup('prepare merchant manager storage state', async ({ page }) => {
  mkdirSync('tests/.auth', { recursive: true })

  if (process.env.PLAYWRIGHT_USE_REAL_KEYCLOAK === 'true') {
    await page.goto('/login')
    const keycloakButton = page.getByRole('button', { name: /continue to keycloak/i })
    if (await keycloakButton.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await keycloakButton.click()
    }
    await page.getByLabel('Username or email').fill(
      process.env.PLAYWRIGHT_MERCHANT_MANAGER_USERNAME || 'merchant.manager',
    )
    await page.getByRole('textbox', { name: 'Password' }).fill(
      process.env.PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD || 'merchant.manager',
    )
    await page.getByRole('button', { name: /sign in/i }).click()
    await page.waitForURL('**/admin/**')
    await page.context().storageState({ path: authFile })
    return
  }

  writeFileSync(authFile, JSON.stringify({ cookies: [], origins: [] }, null, 2))
})
