import { merchantAlphaId, platformAdminAccount } from '../auth/accounts'
import { test, expect } from '../fixtures'
import { BffClient } from '../api/bff-client'
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

test('login page matches ARIA snapshot', { tag: ['@a11y'] }, async ({ app }) => {
  await app.login.goto()
  await app.login.expectLoaded()
  await expect(app.page.getByTestId('auth-required-surface')).toMatchAriaSnapshot()
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

test('guest BFF merchants GET and POST return 401', { tag: ['@security'] }, async ({ app }) => {
  const getResponse = await app.page.request.get('/api/merchants')
  expect(getResponse.status()).toBe(401)
  const postResponse = await app.page.request.post('/api/merchants', {
    data: { merchantReference: 'GUEST-NO-SESSION', displayName: 'Guest' },
  })
  expect(postResponse.status()).toBe(401)
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
  await app.page.getByLabel('Username or email').waitFor({ state: 'visible', timeout: 30_000 })
  await app.page.getByLabel('Username or email').fill(account.username)
  await app.page.getByRole('textbox', { name: 'Password' }).fill(account.password)
  await app.page.getByRole('button', { name: /sign in/i }).click()
  await app.page.waitForURL('**/admin/users', { timeout: 30_000 })
  await expect(app.page.getByRole('heading', { name: 'Users' })).toBeVisible({ timeout: 30_000 })
})
