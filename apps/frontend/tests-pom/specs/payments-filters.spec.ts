import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

test('list filters by amount with API oracle and BFF composition', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const lowRef = uniqueOrderReference(testInfo, 'LOW')
  const highRef = uniqueOrderReference(testInfo, 'HIGH')
  const low = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 1100, currency: 'PLN', clientOrderReference: lowRef },
    uniqueIdempotencyKey(testInfo, 'LOW'),
  )
  const high = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 8800, currency: 'PLN', clientOrderReference: highRef },
    uniqueIdempotencyKey(testInfo, 'HIGH'),
  )
  expect(low.status).toBe(201)
  expect(high.status).toBe(201)

  const listRequest = page.waitForRequest((request) => {
    if (request.method() !== 'GET') {
      return false
    }
    try {
      const url = new URL(request.url())
      return url.pathname === `/api/merchants/${merchantAlphaId}/payment-orders`
        && !url.pathname.endsWith('/summary')
    }
    catch {
      return false
    }
  })
  await app.payments.gotoForMerchant(merchantAlphaId)
  const seen = await listRequest
  expect(new URL(seen.url()).port, 'list must hit Nuxt BFF :3000').toBe('3000')
  await app.payments.expectLoaded()

  await app.payments.applyAmountFilter(5000, 20000)
  await app.payments.expectReferenceVisible(highRef)
  await app.payments.expectReferenceHidden(lowRef)

  const oracle = await client.listPaymentOrders(merchantAlphaId, { minAmount: 5000, maxAmount: 20000 })
  expect(oracle.status).toBe(200)
  const refs = (oracle.body?.content ?? []).map(row => row.clientOrderReference)
  expect(refs).toContain(highRef)
  expect(refs).not.toContain(lowRef)
  await expectNoTokenInBrowserStorage(app.page)
})

test('date status and reference filters agree with API oracle', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'PAIR')
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 2500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PAIR'),
  )
  expect(created.status).toBe(201)
  const today = todayIso()

  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  await app.payments.applyDateFilter(today, today)
  await app.payments.applyStatusFilter('Created')
  await app.payments.filterByClientReference(reference)
  await expect(app.page).toHaveURL(new RegExp(`status=CREATED`))
  await expect(app.page).toHaveURL(new RegExp(`clientOrderReference=${reference}`))
  await app.payments.expectReferenceVisible(reference)

  const oracle = await client.listPaymentOrders(merchantAlphaId, {
    fromDate: today,
    toDate: today,
    status: 'CREATED',
    clientOrderReference: reference,
  })
  expect(oracle.status).toBe(200)
  expect(oracle.body?.content?.some(row => row.clientOrderReference === reference)).toBe(true)
})

test('status and currency pairwise filter agrees with API oracle', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const plnRef = uniqueOrderReference(testInfo, 'PLN')
  const eurRef = uniqueOrderReference(testInfo, 'EUR')
  const pln = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: plnRef },
    uniqueIdempotencyKey(testInfo, 'PLN'),
  )
  const eur = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 2200, currency: 'EUR', clientOrderReference: eurRef },
    uniqueIdempotencyKey(testInfo, 'EUR'),
  )
  expect(pln.status).toBe(201)
  expect(eur.status).toBe(201)

  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  await app.payments.applyStatusFilter('Created')
  await app.payments.applyCurrencyFilter('PLN')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await expect(app.page).toHaveURL(/currency=PLN/)
  await app.payments.expectReferenceVisible(plnRef)
  await app.payments.expectReferenceHidden(eurRef)

  const oracle = await client.listPaymentOrders(merchantAlphaId, { status: 'CREATED', currency: 'PLN' })
  expect(oracle.status).toBe(200)
  const refs = (oracle.body?.content ?? []).map(row => row.clientOrderReference)
  expect(refs).toContain(plnRef)
  expect(refs).not.toContain(eurRef)
})

test('applying filters from a stale page query resets to page 0', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'PAGE')
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 1300, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PAGE'),
  )
  expect(created.status).toBe(201)

  await app.payments.gotoForMerchant(merchantAlphaId, '?page=1')
  await app.payments.expectLoaded()
  const listPath = `/api/merchants/${merchantAlphaId}/payment-orders`
  const reset = page.waitForRequest((request) => {
    if (request.method() !== 'GET') {
      return false
    }
    try {
      const url = new URL(request.url())
      if (url.pathname !== listPath) {
        return false
      }
      const pageParam = url.searchParams.get('page')
      return pageParam === null || pageParam === '0'
    }
    catch {
      return false
    }
  })
  await app.payments.filterByClientReference(reference)
  const seen = await reset
  const pageParam = new URL(seen.url()).searchParams.get('page')
  expect(pageParam === null || pageParam === '0').toBe(true)
  await expect(app.page).not.toHaveURL(/page=1/)
  await app.payments.expectReferenceVisible(reference)
})
