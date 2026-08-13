import { test, expect } from '../fixtures'
import { expectSessionCookieHttpOnly, expectNoTokenInBrowserStorage } from '../utils/storage-safety'

test('session lab shows HttpOnly policy vs empty document.cookie', { tag: ['@security'] }, async ({ app }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  await expect(app.page.getByTestId('session-lab-js-cookies')).toHaveText(/empty|HttpOnly|^$/i)
  await expect(app.page.getByTestId('session-lab-cookie-policy')).toContainText('nuxt-session')
  await expect(app.page.getByTestId('session-lab-cookie-policy')).toContainText('httpOnly')
  await expectSessionCookieHttpOnly(app.page)
  await expectNoTokenInBrowserStorage(app.page)
})

test('idle lock uses page.clock without waitForTimeout', { tag: ['@security'] }, async ({ app }) => {
  await app.page.clock.install()
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  await app.page.clock.fastForward(121_000)
  await expect(app.page.getByTestId('session-lab-idle-lock')).toBeVisible()
  await app.page.getByTestId('session-lab-idle-unlock').click()
  await expect(app.page).toHaveURL(/\/login/)
})

test('csrf demo without token returns 403 csrf_failed', { tag: ['@security'] }, async ({ app }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const responsePromise = app.page.waitForResponse(response =>
    response.url().includes('/api/session-lab/csrf-demo') && response.request().method() === 'POST')
  await app.page.getByTestId('session-lab-csrf-fail').click()
  const response = await responsePromise
  expect(response.status()).toBe(403)
  const body = await response.json()
  expect(body.error).toBe('csrf_failed')
})

test('two contexts sharing storageState can revoke a device', { tag: ['@security'] }, async ({ browser }) => {
  const { pomAuthFiles } = await import('../utils/env')
  const first = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const second = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const page1 = await first.newPage()
  const page2 = await second.newPage()
  try {
    await page1.goto('/admin/session-lab')
    await page2.goto('/admin/session-lab')
    await expect(page1.getByTestId('session-lab-device-list')).toBeVisible()
    await expect(page2.getByTestId('session-lab-device-list')).toBeVisible()
    const revoke = page1.locator('[data-testid^="session-lab-revoke-"]').first()
    await expect(revoke).toBeVisible()
    await revoke.click()
  }
  finally {
    await first.close()
    await second.close()
  }
})
