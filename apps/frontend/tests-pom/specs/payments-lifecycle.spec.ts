import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { requestHeader } from '../utils/network'
import { waitForBffRequest, waitForBffResponse } from '../utils/wait-bff'
import { lifecycleStDtE2e } from '../methods/combinations/LifecycleStDt'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { utcToday } from '../utils/dates'
import { etagOf } from '../utils/http'

test('authorize then capture from the payment detail drawer with If-Match', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = api
  expect(lifecycleStDtE2e.map(edge => edge.id)).toEqual(expect.arrayContaining(['SCN-LIF-01', 'SCN-LIF-02']))
  const reference = uniqueOrderReference(testInfo, 'LIFE')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CREATE'),
  )
  expectStatus(created, 201)
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
  await app.paymentDetail.lifecycleAmountInput().fill('2100')
  const drawerIfMatch = await app.paymentDetail.ifMatchValue()
  expect(drawerIfMatch, 'capture drawer must pre-fill If-Match').toBeTruthy()
  await app.paymentDetail.submitLifecycle()
  expect(requestHeader(await captureIfMatch, 'If-Match')).toBe(drawerIfMatch)
  await expect(app.paymentDetail.statusInDetail('Captured')).toBeVisible()
  await app.paymentDetail.openHistoryTab()
  await expect(app.paymentDetail.historyTab()).toHaveAttribute('aria-selected', 'true')
  await expect(app.paymentDetail.emptyHistory()).toHaveCount(0)
  await expect(app.paymentDetail.historyEntry('CREATED → AUTHORIZED · AUTHORIZE · System')).toBeVisible()
  await expect(app.paymentDetail.historyEntry('AUTHORIZED → CAPTURED · CAPTURE · System')).toBeVisible()
  await expectNoTokenInBrowserStorage(app.page)
})

test('stale If-Match on authorize shows 412 problem+json in the drawer', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'STALE')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1100, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'STALE'),
  )
  expectStatus(created, 201)
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
  await expect(app.problem.statusBadge(412)).toBeVisible()

  const stillCreated = await client.payments.get(ownedMerchantId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')
  const apiStale = await client.payments.authorize(
    ownedMerchantId,
    paymentOrderId,
    '"v99"',
    uniqueIdempotencyKey(testInfo, 'API412'),
  )
  expectStatus(apiStale, 412)
})

test('cancel from CREATED uses ConfirmModal', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'CAN')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CAN'),
  )
  expectStatus(created, 201)

  await app.paymentDetail.gotoOrder(ownedMerchantId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.cancel()
  await expect(app.paymentDetail.statusInDetail('Cancelled')).toBeVisible()
})

test('merchant manager does not see the internal notes form', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'NONOTE')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 700, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'NONOTE'),
  )
  expectStatus(created, 201)

  await app.paymentDetail.gotoOrder(ownedMerchantId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.notesForm()).toHaveCount(0)
})

test('list filters by date, status, and client reference in the query string', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'FILT')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 999, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'FILT'),
  )
  expectStatus(created, 201)

  const today = utcToday()
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyDateRange(today, today)
  await expect(app.page).toHaveURL(new RegExp(`fromDate=${today}`))
  await expect(app.page).toHaveURL(new RegExp(`toDate=${today}`))
  await app.payments.filters.applyStatus('Created')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await app.payments.filters.applyClientReference(reference)
  await expect(app.page).toHaveURL(new RegExp(`clientOrderReference=${reference}`))
  await expect(app.payments.referenceInTable(reference)).toBeVisible()
})

test('copy payment reference writes the client order reference to the clipboard', async ({
  app,
  api,
  context,
  ownedMerchantId,
}, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'CLIP')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 600, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CLIP'),
  )
  expectStatus(created, 201)
  await context.grantPermissions(['clipboard-read', 'clipboard-write'])
  await app.paymentDetail.gotoOrder(ownedMerchantId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.copyClientReference()
  expect(await app.page.evaluate(() => navigator.clipboard.readText())).toBe(reference)
})

test('capture amount above authorized in the drawer is 422 and stays Authorized', async ({
  app,
  api,
  page,
  ownedMerchantId,
}, testInfo) => {
  const client = api
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'CAPUI') },
    uniqueIdempotencyKey(testInfo, 'CAPUI'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const authorized = await client.payments.authorize(
    ownedMerchantId,
    paymentOrderId,
    etagOf((await client.payments.get(ownedMerchantId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'CAPUI-A'),
  )
  expectStatus(authorized, 200)

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  const captureResponse = page.waitForResponse(response =>
    response.request().method() === 'POST'
    && response.url().includes(`/payment-orders/${paymentOrderId}/capture`),
  )
  await app.paymentDetail.capture(2101)
  expect((await captureResponse).status()).toBe(422)
  await app.problem.expectVisible()
  await expect(app.paymentDetail.statusInDetail('Authorized')).toBeVisible()
})
