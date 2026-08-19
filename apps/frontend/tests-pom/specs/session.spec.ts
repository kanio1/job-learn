import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import {
  expectNoJwtInStorageStateFile,
  expectNoTokenInBrowserStorage,
  expectSessionCookieHttpOnly,
  expectSessionCookieSameSiteLax,
  expectSessionCookieUnderUaLimit,
} from '../utils/storage-safety'

test.use({ storageState: pomAuthFiles.platformAdminSession })

test('two contexts sharing storageState can revoke a device', { tag: ['@security'] }, async ({ browser }) => {
  const first = await browser.newContext({ storageState: pomAuthFiles.platformAdminSession })
  const second = await browser.newContext({ storageState: pomAuthFiles.platformAdminSession })
  const page1 = await first.newPage()
  const page2 = await second.newPage()
  try {
    await page1.goto('/admin/session-lab')
    await page2.goto('/admin/session-lab')
    await expect(page1.getByTestId('session-lab-device-list')).toBeVisible()
    await expect(page2.getByTestId('session-lab-device-list')).toBeVisible()
    const revoke = page1.getByRole('button', { name: 'Revoke' }).first()
    await expect(revoke).toBeVisible()
    await revoke.click()
    await expect(page1.getByTestId('session-lab-device-list')).toBeVisible()
  }
  finally {
    await first.close()
    await second.close()
  }
})

test('logout returns to login and blocks admin again', { tag: ['@security', '@serial'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.userMenu.signOut()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()

  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('second logout from login stays on login (EG-W2-11)', { tag: ['@security'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.userMenu.signOut()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
  await expect(app.page.getByTestId('logout-control')).toHaveCount(0)
  await app.login.goto()
  await app.login.expectLoaded()
  await expect(app.page.getByTestId('logout-control')).toHaveCount(0)
  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('session cookie is HttpOnly and storageState has no JWT', { tag: ['@security'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expectSessionCookieHttpOnly(app.page)
  await expectSessionCookieSameSiteLax(app.page)
  await expectSessionCookieUnderUaLimit(app.page)
  await expectNoTokenInBrowserStorage(app.page)
  expectNoJwtInStorageStateFile(pomAuthFiles.platformAdminSession)
})

test('Sign out does not call Keycloak end_session (EG-W2-11 / E2E-027)', { tag: ['@security'] }, async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  let keycloakLogout = 0
  page.on('request', (request) => {
    if (request.url().includes('/protocol/openid-connect/logout')) {
      keycloakLogout += 1
    }
  })
  await app.userMenu.signOut()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
  expect(keycloakLogout, 'application Sign out must not hit RP end_session').toBe(0)
})

test('Session Lab end-session JSON has client_id and no id_token_hint', { tag: ['@security'] }, async ({ app, page }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const posted = await page.request.post('/api/session-lab/end-session')
  expect(posted.status()).toBe(200)
  const body = await posted.json() as { ended?: boolean, endSessionUrl?: string }
  expect(body.ended).toBe(true)
  expect(body.endSessionUrl).toBeTruthy()
  const fromBody = new URL(body.endSessionUrl!)
  expect(fromBody.searchParams.get('client_id')).toBeTruthy()
  expect(fromBody.searchParams.get('post_logout_redirect_uri')).toBeTruthy()
  expect(fromBody.searchParams.has('id_token_hint')).toBe(false)
  await page.goto('/admin/merchants')
  await expect(page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('Session Lab end OIDC posts a logout URL without id_token_hint then hops to Keycloak', { tag: ['@security'] }, async ({ app, page }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()

  const hopped = page.waitForURL(/\/protocol\/openid-connect\/logout/, { timeout: 15_000 })
  await app.sessionLab.endOidc()
  await hopped
  const endSessionUrl = new URL(page.url())
  expect(endSessionUrl.searchParams.get('client_id')).toBeTruthy()
  expect(endSessionUrl.searchParams.get('post_logout_redirect_uri')).toBeTruthy()
  expect(endSessionUrl.searchParams.has('id_token_hint')).toBe(false)

  const confirm = page.getByRole('button', { name: /^(logout|yes|continue)$/i })
  if (await confirm.count() > 0) {
    await confirm.first().click()
  }
  await expect(page).toHaveURL(/\/login/, { timeout: 15_000 })
  await app.login.expectLoaded()
  await page.goto('/admin/merchants')
  await expect(page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
  await expectNoTokenInBrowserStorage(page)
})
