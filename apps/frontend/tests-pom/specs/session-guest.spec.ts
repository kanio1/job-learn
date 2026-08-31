import { merchantAlphaId, platformAdminAccount } from '../auth/accounts'
import { test, expect } from '../fixtures'
import { BffClient } from '../api/bff-client'
import { expectError } from '../api/contracts/http-result'
import { isIpv4LoopbackUrl } from '../methods/error-guessing/OverlayAndIpv6'
import { guestToLoginPaths } from '../methods/use-case/GuestToLoginJourney'

async function expectLoginRedirect(
  app: { page: import('@playwright/test').Page, login: { expectLoaded: () => Promise<void> } },
  path: string,
) {
  await app.page.goto(path)
  await expect(app.page).toHaveURL(/\/login\?redirectTo=/)
  expect(new URL(app.page.url()).searchParams.get('redirectTo')).toBe(path)
  await app.login.expectLoaded()
}

test('BffClient default host is IPv4 loopback (EG-W2-02)', { tag: ['@security'] }, async () => {
  expect(isIpv4LoopbackUrl(BffClient.DEFAULT_BASE_URL)).toBe(true)
})

test('Actor Factory releases guest UI and BFF resources when actor work throws', async ({ actors }) => {
  let actor: Awaited<ReturnType<typeof actors.open>> | undefined
  try {
    actor = await actors.open('guest')
    throw new Error('deliberate actor-work failure')
  }
  catch (error) {
    expect(error).toHaveProperty('message', 'deliberate actor-work failure')
  }
  finally {
    await actors.dispose()
  }

  expect(actor?.page.isClosed()).toBe(true)
  await expect(actor?.api.merchants.list()).rejects.toThrow(/disposed|closed/i)
})

test('login page matches ARIA snapshot', { tag: ['@a11y'] }, async ({ app }) => {
  await app.login.goto()
  await app.login.expectLoaded()
  await expect(app.login.authRequiredSurface()).toMatchAriaSnapshot()
})

test('unauthenticated visit to merchants lands on login', { tag: ['@security'] }, async ({ app }) => {
  expect(guestToLoginPaths[0].path).toBe('/admin/merchants')
  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login\?redirectTo=/)
  await app.login.expectLoaded()
})

test('unauthenticated visit to session lab lands on login', { tag: ['@security'] }, async ({ app }) => {
  await app.page.goto('/admin/session-lab')
  await expect(app.page).toHaveURL(/\/login\?redirectTo=/)
  await app.login.expectLoaded()
})

test('unauthenticated admin and lab paths land on login with redirectTo', { tag: ['@security'] }, async ({ app }) => {
  const paymentsPath = `/admin/merchants/${merchantAlphaId}/payments`
  await expectLoginRedirect(app, '/admin/users')
  await expectLoginRedirect(app, paymentsPath)
  await expectLoginRedirect(app, '/error-lab')
  await expectLoginRedirect(app, '/admin/checkout-lab')
})

test('guest BFF merchants GET and POST return 401 without credential fallback', { tag: ['@security'] }, async ({ api }) => {
  expectError(await api.merchants.list(), 401)
  expectError(await api.merchants.create('GUEST-NO-SESSION', 'Guest'), 401)
})

test('guest POST session-lab csrf-demo returns 401', { tag: ['@security'] }, async ({ app }) => {
  const response = await app.page.request.post('/api/session-lab/csrf-demo')
  expect(response.status()).toBe(401)
})

test('login with redirectTo returns to the intended admin path', { tag: ['@security'] }, async ({ app }) => {
  test.setTimeout(60_000)
  const account = platformAdminAccount()
  await app.page.goto('/admin/users')
  await expect(app.page).toHaveURL(/\/login\?redirectTo=/)
  await app.login.expectLoaded()
  await app.login.continueToKeycloak()
  await app.login.keycloakUsernameOrEmail().waitFor({ state: 'visible', timeout: 30_000 })
  await app.login.keycloakUsernameOrEmail().fill(account.username)
  await app.login.keycloakPassword().fill(account.password)
  await app.login.keycloakSubmit().click()
  await app.page.waitForURL('**/admin/users', { timeout: 30_000 })
  await expect(app.users.heading()).toBeVisible({ timeout: 30_000 })
})
