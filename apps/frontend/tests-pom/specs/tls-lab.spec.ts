import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectNoTokenInBrowserStorage, expectSessionCookieSameSiteLax, expectSessionCookieSecure } from '../utils/storage-safety'
import { expectStatus } from '../api/bff-client'
import { requestHeader } from '../utils/network'
import { waitForBffRequest, waitForBffResponse } from '../utils/wait-bff'

const OTHER_ITEM = '00000000-0000-0000-0000-0000000000a2'

test('platform admin reaches RLS lab hub over the TLS origin', async ({ app }) => {
  await app.rlsLab.goto()
  await app.rlsLab.expectLoaded()
  expect(new URL(app.page.url()).protocol).toBe('https:')
  await expect(app.page).toHaveURL(/\/admin\/rls-lab/)
  await expectNoTokenInBrowserStorage(app.page)
})

test('platform admin compare shows unprotected leak over the TLS origin', async ({ app, api }) => {
  const client = api
  await app.rlsLab.goto()
  await app.rlsLab.expectLoaded()
  await expect(app.rlsLab.comparePanel()).toBeVisible()
  await app.rlsLab.loadCompare()
  await expect(app.rlsLab.restrictedWithoutTenant()).toHaveText('0')
  const unprotected = Number(await app.rlsLab.unprotectedCount().innerText())
  expect(unprotected).toBeGreaterThan(0)
  const compare = await client.labs.rlsCompare()
  expectStatus(compare, 200)
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

test('platform admin Keycloak rejects a redirect URI outside the realm', async ({ app, page }) => {
  const issuer = process.env.PLAYWRIGHT_KEYCLOAK_ISSUER
    || 'https://auth.payment-quality.local:8443/realms/payment-quality'
  const authorize = `${issuer}/protocol/openid-connect/auth?client_id=payment-quality-dashboard&redirect_uri=${encodeURIComponent('https://evil.example/callback')}&response_type=code&scope=openid`
  const response = await page.goto(authorize)
  expect(response?.status()).toBe(400)
  expect(new URL(page.url()).origin).toBe(new URL(issuer).origin)
  await expect(app.login.keycloakProtocolError()).toBeVisible()
})

test('merchant manager applies a payment amount filter over the TLS origin', async ({ app, api }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'TLS')
  const created = await client.payments.createOrder(
    merchantAlphaId,
    { amountMinor: 7700, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'TLS'),
  )
  expectStatus(created, 201)

  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyAmountRange(5000, 20000)
  await expect(app.page).toHaveURL(/minAmount=5000/)
  await expect(app.payments.referenceInTable(reference)).toBeVisible()
  await expectNoTokenInBrowserStorage(app.page)
})

test('merchant manager authorizes then captures over the TLS origin', async ({ app, api, page }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'TLSLIFE')
  const created = await client.payments.createOrder(
    merchantAlphaId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'TLSLIFE'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const detailPath = `/api/merchants/${merchantAlphaId}/payment-orders/${paymentOrderId}`

  const initialDetail = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  const initial = await initialDetail
  const initialEtag = initial.headers()['etag']
  expect(initialEtag, 'Caddy must preserve the origin ETag').toMatch(/^"v\d+"$/)
  expect(initial.headers()['cache-control'] ?? '').toMatch(/no-transform/i)
  expect(initial.headers()['content-encoding']).toBeUndefined()
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.openLifecycle('authorize')
  await app.paymentDetail.fillIfMatch('"v99"')
  const stale = page.waitForResponse(response =>
    response.request().method() === 'POST'
    && response.url().includes(`/payment-orders/${paymentOrderId}/authorize`),
  )
  await app.paymentDetail.submitLifecycle()
  expect((await stale).status()).toBe(412)
  const stillCreated = await client.payments.get(merchantAlphaId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')

  const reloadedDetail = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
  await app.page.reload()
  const reloaded = await reloadedDetail
  const reloadedEtag = reloaded.headers()['etag']
  expect(reloadedEtag, 'reloaded TLS detail must retain an origin ETag').toMatch(/^"v\d+"$/)
  expect(reloaded.headers()['cache-control'] ?? '').toMatch(/no-transform/i)
  await app.paymentDetail.expectLoaded()
  const authorizeRequest = waitForBffRequest(page, { method: 'POST', pathExact: `${detailPath}/authorize` })
  const authorizeResponse = waitForBffResponse(page, { method: 'POST', pathExact: `${detailPath}/authorize` })
  await app.paymentDetail.authorize()
  expect(requestHeader(await authorizeRequest, 'If-Match')).toBe(reloadedEtag)
  const authorized = await authorizeResponse
  const authorizedEtag = authorized.headers()['etag']
  expect(authorizedEtag, 'authorized TLS response must retain an origin ETag').toMatch(/^"v\d+"$/)
  expect(authorized.headers()['cache-control'] ?? '').toMatch(/no-transform/i)
  await expect(app.paymentDetail.statusInDetail('Authorized')).toBeVisible()
  const captureRequest = waitForBffRequest(page, { method: 'POST', pathExact: `${detailPath}/capture` })
  await app.paymentDetail.capture(2100)
  expect(requestHeader(await captureRequest, 'If-Match')).toBe(authorizedEtag)
  await expect(app.paymentDetail.statusInDetail('Captured')).toBeVisible()
  expect(new URL(app.page.url()).protocol).toBe('https:')
})

test('merchant manager dismissing ConfirmModal does not cancel over TLS', async ({ app, api, page }, testInfo) => {
  const client = api
  const created = await client.payments.createOrder(
    merchantAlphaId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'TLSDISC') },
    uniqueIdempotencyKey(testInfo, 'TLSDISC'),
  )
  expectStatus(created, 201)
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
    await expect(app.paymentDetail.confirm.heading(/Confirm Cancel/)).toHaveCount(0)
    expect(cancelPosted).toBe(false)
  }
  finally {
    page.off('request', onRequest)
  }
  const stillCreated = await client.payments.get(merchantAlphaId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')
})

test('merchant manager sees only Alpha row over the TLS origin', async ({ actors }) => {
  const { page, app, api } = await actors.open('merchantManager', {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'https://app.payment-quality.local:8443',
    ignoreHTTPSErrors: process.env.PLAYWRIGHT_TLS_INSECURE === '1',
  })
    await app.rlsLab.goto()
    await app.rlsLab.expectLoaded()
    await expect(app.rlsLab.item('Alpha secret')).toBeVisible()
    await expect(app.rlsLab.item('Other tenant secret')).toHaveCount(0)
    await app.rlsLab.probe(OTHER_ITEM)
    await app.problem.expectVisible()
    await expect(app.problem.errorCode()).toHaveText('not_found')
    const compare = await api.labs.rlsCompare()
    expectStatus(compare, 403)
    expect(new URL(page.url()).protocol).toBe('https:')
})
