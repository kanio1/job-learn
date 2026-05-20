import { test as setup } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'node:fs'

const authFile = 'tests/.auth/platform-operator.json'

setup('prepare platform operator storage state', async ({ page }) => {
  mkdirSync('tests/.auth', { recursive: true })

  if (process.env.PLAYWRIGHT_USE_REAL_KEYCLOAK === 'true') {
    await page.goto('/login')
    const keycloakButton = page.getByRole('button', { name: /continue to keycloak/i })
    if (await keycloakButton.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await keycloakButton.click()
    }
    await page.getByLabel('Username or email').fill(process.env.PLAYWRIGHT_KEYCLOAK_USERNAME || 'platform.operator')
    await page.getByRole('textbox', { name: 'Password' }).fill(process.env.PLAYWRIGHT_KEYCLOAK_PASSWORD || 'platform.operator')
    await page.getByRole('button', { name: /sign in/i }).click()

    const updateProfile = page.getByRole('heading', { name: /update account information/i })
    if (await updateProfile.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await page.getByLabel('Email').fill('platform.operator@example.test')
      await page.getByLabel('First name').fill('Platform')
      await page.getByLabel('Last name').fill('Operator')
      await page.getByRole('button', { name: /submit/i }).click()
    }

    await page.waitForURL('**/admin/merchants')
    await page.context().storageState({ path: authFile })
    return
  }

  writeFileSync(authFile, JSON.stringify({ cookies: [], origins: [] }, null, 2))
})
