import { test, expect } from '../fixtures'
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
import { z } from 'zod'

const revokedDeviceSchema = z.object({ revoked: z.boolean().optional() }).passthrough()
const deviceListSchema = z.array(z.object({ id: z.string() }).passthrough())
const endSessionSchema = z.object({ ended: z.boolean().optional(), endSessionUrl: z.string().url().optional() }).passthrough()
const sessionSchema = z.object({ user: z.unknown().optional() }).passthrough()

test.use({ storageState: pomAuthFiles.platformAdminSession })

test('two contexts sharing storageState can revoke a device', { tag: ['@security'] }, async ({ actors }) => {
  const first = await actors.open('platformAdminSession')
  const second = await actors.open('platformAdminSession')
  const { page: page1 } = first
  const { page: page2 } = second
  const lab1 = first.app.sessionLab
  const lab2 = second.app.sessionLab
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
    expect(revokedDeviceSchema.parse(await revoked.json()).revoked).toBe(true)
    await expect(lab1.revokeButton(extraId)).toHaveCount(0)

    const after1 = await page1.request.get('/api/session-lab/devices')
    const after2 = await page2.request.get('/api/session-lab/devices')
    expect(after1.status()).toBe(200)
    expect(after2.status()).toBe(200)
    const ids1 = deviceListSchema.parse(await after1.json()).map(device => device.id)
    const ids2 = deviceListSchema.parse(await after2.json()).map(device => device.id)
    expect(ids1).not.toContain(extraId)
    expect(ids2).not.toContain(extraId)
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
  await expect(app.userMenu.control()).toHaveCount(0)
  await app.login.goto()
  await app.login.expectLoaded()
  await expect(app.userMenu.control()).toHaveCount(0)
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
  await app.userMenu.control().click()
  await app.userMenu.signOutOption().click()
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
  await expect(app.login.keycloakHeading()).toBeVisible()
  await expect(app.login.keycloakUsername()).toBeVisible()
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
  await expect(app.login.keycloakHeading()).toBeVisible()
})

test('Session Lab end-session JSON has client_id and no id_token_hint', { tag: ['@security'] }, async ({ app, page }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const posted = await page.request.post('/api/session-lab/end-session')
  expect(posted.status()).toBe(200)
  const body = endSessionSchema.parse(await posted.json())
  expect(body.ended).toBe(true)
  expect(body.endSessionUrl).toBeTruthy()
  if (!body.endSessionUrl) {
    throw new Error('end-session response must include endSessionUrl')
  }
  const fromBody = new URL(body.endSessionUrl)
  expect(fromBody.searchParams.get('client_id')).toBeTruthy()
  expect(fromBody.searchParams.get('post_logout_redirect_uri')).toBeTruthy()
  expect(fromBody.searchParams.has('id_token_hint')).toBe(false)

  const session = await page.request.get('/api/_auth/session')
  expect(session.status()).toBe(200)
  const sessionBody = sessionSchema.parse(await session.json())
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
