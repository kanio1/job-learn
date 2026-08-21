import { expect, type Page } from '@playwright/test'

/**
 * Finish Keycloak RP-Initiated Logout when the confirm page is shown
 * (lab has no id_token_hint). No-op if the browser is already on /login.
 */
export async function completeKeycloakEndSession(page: Page): Promise<void> {
  await page.waitForURL(/\/protocol\/openid-connect\/logout|\/login/, { timeout: 15_000 })
  if (page.url().includes('/protocol/openid-connect/logout')) {
    const confirm = page.getByRole('button', { name: /^(logout|yes|continue)$/i })
    await expect(confirm.or(page.getByRole('heading', { name: /sign in|log in/i }))).toBeVisible({ timeout: 15_000 })
    if (await confirm.isVisible()) {
      await confirm.click()
    }
  }
  await expect(page).toHaveURL(/\/login/, { timeout: 15_000 })
}
