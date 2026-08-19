import { test, expect } from '../fixtures'
import { expectSessionCookieHttpOnly, expectNoTokenInBrowserStorage } from '../utils/storage-safety'

test('session lab shows HttpOnly policy vs empty document.cookie', { tag: ['@security'] }, async ({ app }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const jsCookies = await app.page.getByTestId('session-lab-js-cookies').innerText()
  expect(jsCookies.includes('nuxt-session'), 'HttpOnly nuxt-session must not appear in document.cookie').toBe(false)
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

