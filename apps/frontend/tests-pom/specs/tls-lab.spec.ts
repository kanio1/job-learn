import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectNoTokenInBrowserStorage, expectSessionCookieSameSiteLax, expectSessionCookieSecure } from '../utils/storage-safety'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'
import { BffClient } from '../api/bff-client'

const OTHER_ITEM = '00000000-0000-0000-0000-0000000000a2'

test('platform admin reaches RLS lab hub over the TLS origin', async ({ app }) => {
  await app.rlsLab.goto()
  await app.rlsLab.expectLoaded()
  expect(new URL(app.page.url()).protocol).toBe('https:')
  await expect(app.page).toHaveURL(/\/admin\/rls-lab/)
  await expectNoTokenInBrowserStorage(app.page)
})

test('platform admin compare shows unprotected leak over the TLS origin', async ({ app, api }) => {
  const client = requireApi(api)
  await app.rlsLab.goto()
  await app.rlsLab.expectLoaded()
  await expect(app.page.getByTestId('rls-lab-compare-panel')).toBeVisible()
  await app.rlsLab.loadCompare()
  await expect(app.page.getByTestId('rls-lab-compare-restricted-no-tenant')).toHaveText('0')
  const unprotected = Number(await app.page.getByTestId('rls-lab-compare-unprotected').innerText())
  expect(unprotected).toBeGreaterThan(0)
  const compare = await client.rlsCompare()
  expect(compare.status).toBe(200)
  expect(compare.body?.restrictedWithoutTenantGuc).toBe(0)
  await expectNoTokenInBrowserStorage(app.page)
})

test('platform admin has a Secure session cookie on the TLS origin', async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expectSessionCookieSecure(app.page, true)
  await expectSessionCookieSameSiteLax(app.page)
  await expectNoTokenInBrowserStorage(app.page)
})

test('platform admin Keycloak rejects a redirect URI outside the realm', async ({ page }) => {
  const issuer = process.env.PLAYWRIGHT_KEYCLOAK_ISSUER
    || 'https://auth.payment-quality.local:8443/realms/payment-quality'
  const authorize = `${issuer}/protocol/openid-connect/auth?client_id=payment-quality-dashboard&redirect_uri=${encodeURIComponent('https://evil.example/callback')}&response_type=code&scope=openid`
  await page.goto(authorize)
  await expect(page.getByText(/invalid parameter|redirect.?uri|invalid request/i)).toBeVisible()
})

test('merchant manager applies a payment amount filter over the TLS origin', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'TLS')
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 7700, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'TLS'),
  )
  expect(created.status).toBe(201)

  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  await app.payments.applyAmountFilter(5000, 20000)
  await expect(app.page).toHaveURL(/minAmount=5000/)
  await app.payments.expectReferenceVisible(reference)
  await expectNoTokenInBrowserStorage(app.page)
})

test('merchant manager authorizes then captures over the TLS origin', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'TLSLIFE')
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'TLSLIFE'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!

  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.openLifecycle('authorize')
  await app.paymentDetail.fillIfMatch('"v99"')
  const stale = page.waitForResponse(response =>
    response.request().method() === 'POST'
    && response.url().includes(`/payment-orders/${paymentOrderId}/authorize`),
  )
  await app.paymentDetail.submitLifecycle()
  expect((await stale).status()).toBe(412)
  const stillCreated = await client.getPaymentOrder(merchantAlphaId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')

  await app.page.reload()
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.authorize()
  await expect(app.paymentDetail.statusInDetail('Authorized')).toBeVisible()
  await app.paymentDetail.capture(2100)
  await expect(app.paymentDetail.statusInDetail('Captured')).toBeVisible()
  expect(new URL(app.page.url()).protocol).toBe('https:')
})

test('merchant manager dismissing ConfirmModal does not cancel over TLS', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'TLSDISC') },
    uniqueIdempotencyKey(testInfo, 'TLSDISC'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  let cancelPosted = false
  const onRequest = (request: { method: () => string, url: () => string }) => {
    if (request.method() === 'POST' && request.url().includes(`/payment-orders/${paymentOrderId}/cancel`)) {
      cancelPosted = true
    }
  }
  page.on('request', onRequest)
  try {
    await app.paymentDetail.openCancelThenDismiss()
    await expect(app.page.getByRole('heading', { name: /Confirm Cancel/ })).toHaveCount(0)
    expect(cancelPosted).toBe(false)
  }
  finally {
    page.off('request', onRequest)
  }
  const stillCreated = await client.getPaymentOrder(merchantAlphaId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')
})

test('merchant manager sees only Alpha row over the TLS origin', async ({ browser, playwright }) => {
  const managerContext = await browser.newContext({
    storageState: pomAuthFiles.merchantManager,
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'https://app.payment-quality.local:8443',
    ignoreHTTPSErrors: process.env.PLAYWRIGHT_TLS_INSECURE === '1',
  })
  const page = await managerContext.newPage()
  const app = new App(page)
  const api = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  try {
    await app.rlsLab.goto()
    await app.rlsLab.expectLoaded()
    await expect(page.getByText('Alpha secret')).toBeVisible()
    await expect(page.getByText('Other tenant secret')).toHaveCount(0)
    await app.rlsLab.probe(OTHER_ITEM)
    await app.problem.expectVisible()
    await app.problem.expectError('not_found')
    const compare = await api.rlsCompare()
    expect(compare.status).toBe(403)
    expect(new URL(page.url()).protocol).toBe('https:')
  }
  finally {
    await api.dispose()
    await managerContext.close()
  }
})
