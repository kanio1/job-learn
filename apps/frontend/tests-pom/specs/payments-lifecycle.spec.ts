import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { requestHeader } from '../utils/network'
import { waitForBffRequest, waitForBffResponse } from '../utils/wait-bff'
import { lifecycleStDtE2e } from '../methods/combinations/LifecycleStDt'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { utcToday } from '../utils/dates'

test('authorize then capture from the payment detail drawer with If-Match', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  expect(lifecycleStDtE2e.map(edge => edge.id)).toEqual(expect.arrayContaining(['SCN-LIF-01', 'SCN-LIF-02']))
  const reference = uniqueOrderReference(testInfo, 'LIFE')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CREATE'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()

  const detailPath = `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}`
  const getPromise = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId!)
  const getResponse = await getPromise
  const etag = getResponse.headers()['etag']
  expect(etag, 'GET detail must return ETag').toBeTruthy()
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.statusInDetail('Created')).toBeVisible()

  const authorizeRequest = waitForBffRequest(page, { method: 'POST', pathExact: `${detailPath}/authorize` })
  await app.paymentDetail.authorize()
  const authorize = await authorizeRequest
  expect(requestHeader(authorize, 'If-Match')).toBe(etag)
  expect(requestHeader(authorize, 'Idempotency-Key')).toBeTruthy()
  await expect(app.paymentDetail.statusInDetail('Authorized')).toBeVisible()

  const captureIfMatch = waitForBffRequest(page, { method: 'POST', pathExact: `${detailPath}/capture` })
  await app.paymentDetail.openLifecycle('capture')
  await app.page.getByTestId('lifecycle-amount-input').fill('2100')
  const drawerIfMatch = await app.paymentDetail.ifMatchValue()
  expect(drawerIfMatch, 'capture drawer must pre-fill If-Match').toBeTruthy()
  await app.paymentDetail.submitLifecycle()
  expect(requestHeader(await captureIfMatch, 'If-Match')).toBe(drawerIfMatch)
  await expect(app.paymentDetail.statusInDetail('Captured')).toBeVisible()
  await expectNoTokenInBrowserStorage(app.page)
})

test('stale If-Match on authorize shows 412 problem+json in the drawer', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'STALE')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'STALE'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
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

  const stillCreated = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')
  const apiStale = await client.authorizePayment(
    ownedMerchantId,
    paymentOrderId,
    '"v99"',
    uniqueIdempotencyKey(testInfo, 'API412'),
  )
  expect(apiStale.status).toBe(412)
})

test('cancel from CREATED uses ConfirmModal', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'CAN')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CAN'),
  )
  expect(created.status).toBe(201)

  await app.paymentDetail.gotoOrder(ownedMerchantId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.cancel()
  await expect(app.paymentDetail.statusInDetail('Cancelled')).toBeVisible()
})

test('merchant manager does not see the internal notes form', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'NONOTE')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 700, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'NONOTE'),
  )
  expect(created.status).toBe(201)

  await app.paymentDetail.gotoOrder(ownedMerchantId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('payment-note-body')).toHaveCount(0)
})

test('list filters by date, status, and client reference in the query string', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'FILT')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 999, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'FILT'),
  )
  expect(created.status).toBe(201)

  const today = utcToday()
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.applyDateFilter(today, today)
  await expect(app.page).toHaveURL(new RegExp(`fromDate=${today}`))
  await expect(app.page).toHaveURL(new RegExp(`toDate=${today}`))
  await app.payments.applyStatusFilter('Created')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await app.payments.filterByClientReference(reference)
  await expect(app.page).toHaveURL(new RegExp(`clientOrderReference=${reference}`))
  await app.payments.expectReferenceVisible(reference)
})
