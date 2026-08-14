import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { requestHeader } from '../utils/network'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

test('authorize then capture from the payment detail drawer with If-Match', async ({ app, api, page }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'LIFE')
  const created = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CREATE'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()

  const detailPath = `/api/merchants/${merchantAlphaId}/payment-orders/${paymentOrderId}`
  const getPromise = page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    try {
      const pathname = new URL(response.url()).pathname
      return pathname === `/api/merchants/${merchantAlphaId}/payment-orders/${paymentOrderId}`
    } catch {
      return false
    }
  })
  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId!)
  const getResponse = await getPromise
  const etag = getResponse.headers()['etag']
  expect(etag, 'GET detail must return ETag').toBeTruthy()
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.statusInDetail('Created')).toBeVisible()

  const authorizeRequest = page.waitForRequest(request =>
    request.method() === 'POST' && request.url().includes(`${detailPath}/authorize`),
  )
  await app.paymentDetail.authorize()
  const authorize = await authorizeRequest
  expect(requestHeader(authorize, 'If-Match')).toBe(etag)
  expect(requestHeader(authorize, 'Idempotency-Key')).toBeTruthy()
  await expect(app.paymentDetail.statusInDetail('Authorized')).toBeVisible()

  const captureIfMatch = page.waitForRequest(request =>
    request.method() === 'POST' && request.url().includes(`${detailPath}/capture`),
  )
  await app.paymentDetail.openLifecycle('capture')
  await app.page.getByTestId('lifecycle-amount-input').fill('2100')
  const drawerIfMatch = await app.paymentDetail.ifMatchValue()
  expect(drawerIfMatch, 'capture drawer must pre-fill If-Match').toBeTruthy()
  await app.paymentDetail.submitLifecycle()
  expect(requestHeader(await captureIfMatch, 'If-Match')).toBe(drawerIfMatch)
  await expect(app.paymentDetail.statusInDetail('Captured')).toBeVisible()
  await expectNoTokenInBrowserStorage(app.page)
})

test('stale If-Match on authorize shows 412 problem+json in the drawer', async ({ app, api, page }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'STALE')
  const created = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 1100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'STALE'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!

  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.openLifecycle('authorize')
  await app.paymentDetail.fillIfMatch('"v99"')

  const authorizeResponse = page.waitForResponse(response =>
    response.request().method() === 'POST'
    && response.url().includes(`/payment-orders/${paymentOrderId}/authorize`),
  )
  await app.paymentDetail.submitLifecycle()
  expect((await authorizeResponse).status()).toBe(412)
  await app.problem.expectVisible()
  await app.problem.expectStatusBadge(412)

  const stillCreated = await api.getPaymentOrder(merchantAlphaId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')
  const apiStale = await api.authorizePayment(
    merchantAlphaId,
    paymentOrderId,
    '"v99"',
    uniqueIdempotencyKey(testInfo, 'API412'),
  )
  expect(apiStale.status).toBe(412)
})

test('cancel from CREATED uses ConfirmModal', async ({ app, api }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'CAN')
  const created = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CAN'),
  )
  expect(created.status).toBe(201)

  await app.paymentDetail.gotoOrder(merchantAlphaId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.cancel()
  await expect(app.paymentDetail.statusInDetail('Cancelled')).toBeVisible()
})

test('merchant manager does not see the internal notes form', async ({ app, api }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'NONOTE')
  const created = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 700, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'NONOTE'),
  )
  expect(created.status).toBe(201)

  await app.paymentDetail.gotoOrder(merchantAlphaId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('payment-note-body')).toHaveCount(0)
})

test('list filters by date, status, and client reference in the query string', async ({ app, api }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'FILT')
  const created = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 999, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'FILT'),
  )
  expect(created.status).toBe(201)

  const now = new Date()
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  await app.payments.applyDateFilter(today, today)
  await expect(app.page).toHaveURL(new RegExp(`fromDate=${today}`))
  await expect(app.page).toHaveURL(new RegExp(`toDate=${today}`))
  await app.payments.applyStatusFilter('Created')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await app.payments.filterByClientReference(reference)
  expect(app.page.url()).toContain(`clientOrderReference=${reference}`)
  await app.payments.expectReferenceVisible(reference)
})
