import { test, expect } from '../fixtures'
import { App } from '../pages/App'
import { pomAuthFiles } from '../utils/env'
import { completeKeycloakEndSession } from '../utils/keycloak-oidc'
import {
  expectNoJwtInStorageStateFile,
  expectNoTokenInBrowserStorage,
  expectSessionCookieCleared,
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
  const lab1 = new App(page1).sessionLab
  const lab2 = new App(page2).sessionLab
  try {
    await lab1.goto()
    await lab2.goto()
    await lab1.expectLoaded()
    await lab2.expectLoaded()
    await expect(lab1.deviceList()).toBeVisible()
    await expect(lab2.deviceList()).toBeVisible()

    const extraId = `pom-rev-${crypto.randomUUID()}`
    const created = await page1.request.post('/api/session-lab/devices', {
      data: { id: extraId, label: 'POM extra device' },
    })
    expect(created.status()).toBe(200)

    await page1.reload()
    await lab1.expectLoaded()
    await expect(lab1.revokeButton(extraId)).toBeVisible()

    const before2 = await page2.request.get('/api/session-lab/devices')
    expect(before2.status()).toBe(200)

    const revokeResponse = page1.waitForResponse(response =>
      response.url().includes(`/api/session-lab/devices/${extraId}/revoke`)
      && response.request().method() === 'POST')
    await lab1.revoke(extraId)
    const revoked = await revokeResponse
    expect(revoked.status()).toBe(200)
    expect((await revoked.json() as { revoked?: boolean }).revoked).toBe(true)
    await expect(lab1.revokeButton(extraId)).toHaveCount(0)

    const after1 = await page1.request.get('/api/session-lab/devices')
    const after2 = await page2.request.get('/api/session-lab/devices')
    expect(after1.status()).toBe(200)
    expect(after2.status()).toBe(200)
    const ids1 = (await after1.json() as { id: string }[]).map(device => device.id)
    const ids2 = (await after2.json() as { id: string }[]).map(device => device.id)
    expect(ids1).not.toContain(extraId)
    expect(ids2).not.toContain(extraId)
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
  await expectSessionCookieCleared(app.page)
  await expectNoTokenInBrowserStorage(app.page)

  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('clearing cookies mid-journey on merchants returns to login', { tag: ['@security'] }, async ({ app, context }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await context.clearCookies()
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

test('Sign out hops to Keycloak end_session then login (PW-MRL-E2E-027)', { tag: ['@security'] }, async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()

  const logoutReq = page.waitForRequest(request => request.url().includes('/protocol/openid-connect/logout'))
  await page.getByTestId('logout-control').click()
  await page.getByRole('menuitem', { name: 'Sign out', exact: true }).click()
  const endSessionUrl = new URL((await logoutReq).url())
  expect(endSessionUrl.searchParams.get('client_id')).toBeTruthy()
  expect(endSessionUrl.searchParams.get('post_logout_redirect_uri')).toBeTruthy()
  expect(endSessionUrl.searchParams.has('id_token_hint')).toBe(false)

  await completeKeycloakEndSession(page)
  await app.login.expectLoaded()
  await expectSessionCookieCleared(page)
  await expectNoTokenInBrowserStorage(page)

  await app.login.continueToKeycloak()
  await expect(page).toHaveURL(/\/realms\/payment-quality\/protocol\/openid-connect\/auth/, { timeout: 15_000 })
  expect(new URL(page.url()).searchParams.get('prompt')).toBeNull()
  await expect(page.getByRole('heading', { name: /sign in to your account/i })).toBeVisible()
  await expect(page.getByLabel(/username/i)).toBeVisible()
})

test('Sign out of dashboard only keeps Keycloak SSO and explains resume', { tag: ['@security'] }, async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  let keycloakLogout = 0
  page.on('request', (request) => {
    if (request.url().includes('/protocol/openid-connect/logout')) {
      keycloakLogout += 1
    }
  })
  await app.userMenu.signOutOfDashboardOnly()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
  await expect(app.login.ssoResumeNotice()).toBeVisible()
  await expectSessionCookieCleared(app.page)
  await expectNoTokenInBrowserStorage(app.page)
  expect(keycloakLogout, 'named shallow logout must not hit RP end_session').toBe(0)

  await app.login.useDifferentAccount()
  await expect(page).toHaveURL(/\/protocol\/openid-connect\/auth/, { timeout: 15_000 })
  expect(new URL(page.url()).searchParams.get('prompt')).toBe('login')
  await expect(page.getByRole('heading', { name: /sign in to your account/i })).toBeVisible()
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

  const session = await page.request.get('/api/_auth/session')
  expect(session.status()).toBe(200)
  const sessionBody = await session.json() as { user?: unknown }
  expect(sessionBody.user, 'POST end-session must drop the BFF user').toBeFalsy()

  await page.goto('/admin/merchants')
  await expect(page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
  await expectNoTokenInBrowserStorage(page)
})

test('Session Lab end OIDC hops to Keycloak logout without id_token_hint', { tag: ['@security'] }, async ({ app, page }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()

  const logoutReq = page.waitForRequest(request => request.url().includes('/protocol/openid-connect/logout'))
  await app.sessionLab.endOidc()
  const endSessionUrl = new URL((await logoutReq).url())
  expect(endSessionUrl.searchParams.get('client_id')).toBeTruthy()
  expect(endSessionUrl.searchParams.get('post_logout_redirect_uri')).toBeTruthy()
  expect(endSessionUrl.searchParams.has('id_token_hint')).toBe(false)

  await completeKeycloakEndSession(page)
  await app.login.expectLoaded()
  await expectSessionCookieCleared(page)
  await page.goto('/admin/merchants')
  await expect(page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
  await expectNoTokenInBrowserStorage(page)
})
