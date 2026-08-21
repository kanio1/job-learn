import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { utcToday } from '../utils/dates'
import { etagOf } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'

test('list filters by amount with API oracle and BFF composition', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const lowRef = uniqueOrderReference(testInfo, 'LOW')
  const highRef = uniqueOrderReference(testInfo, 'HIGH')
  const low = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1100, currency: 'PLN', clientOrderReference: lowRef },
    uniqueIdempotencyKey(testInfo, 'LOW'),
  )
  const high = await client.createPaymentOrder(
    ownedMerchantId,
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
      return url.pathname === `/api/merchants/${ownedMerchantId}/payment-orders`
        && !url.pathname.endsWith('/summary')
    }
    catch {
      return false
    }
  })
  await app.payments.gotoForMerchant(ownedMerchantId)
  const seen = await listRequest
  expect(new URL(seen.url()).port, 'list must hit Nuxt BFF :3000').toBe('3000')
  await app.payments.expectLoaded()

  await app.payments.applyAmountFilter(5000, 20000)
  await app.payments.expectReferenceVisible(highRef)
  await app.payments.expectReferenceHidden(lowRef)

  const oracle = await client.listPaymentOrders(ownedMerchantId, { minAmount: 5000, maxAmount: 20000 })
  expect(oracle.status).toBe(200)
  const refs = (oracle.body?.content ?? []).map(row => row.clientOrderReference)
  expect(refs).toContain(highRef)
  expect(refs).not.toContain(lowRef)
  await expectNoTokenInBrowserStorage(app.page)
})

test('PW-M360-E2E-027 date status and reference filters agree with API oracle', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'PAIR')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 2500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PAIR'),
  )
  expect(created.status).toBe(201)
  const today = utcToday()

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.applyDateFilter(today, today)
  await app.payments.applyStatusFilter('Created')
  await app.payments.filterByClientReference(reference)
  await expect(app.page).toHaveURL(new RegExp(`status=CREATED`))
  await expect(app.page).toHaveURL(new RegExp(`clientOrderReference=${reference}`))
  await app.payments.expectReferenceVisible(reference)

  const oracle = await client.listPaymentOrders(ownedMerchantId, {
    fromDate: today,
    toDate: today,
    status: 'CREATED',
    clientOrderReference: reference,
  })
  expect(oracle.status).toBe(200)
  expect(oracle.body?.content?.some(row => row.clientOrderReference === reference)).toBe(true)
})

test('status and currency pairwise filter agrees with API oracle', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const plnRef = uniqueOrderReference(testInfo, 'PLN')
  const eurRef = uniqueOrderReference(testInfo, 'EUR')
  const pln = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: plnRef },
    uniqueIdempotencyKey(testInfo, 'PLN'),
  )
  const eur = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 2200, currency: 'EUR', clientOrderReference: eurRef },
    uniqueIdempotencyKey(testInfo, 'EUR'),
  )
  expect(pln.status).toBe(201)
  expect(eur.status).toBe(201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.applyStatusFilter('Created')
  await app.payments.applyCurrencyFilter('PLN')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await expect(app.page).toHaveURL(/currency=PLN/)
  await app.payments.expectReferenceVisible(plnRef)
  await app.payments.expectReferenceHidden(eurRef)

  const oracle = await client.listPaymentOrders(ownedMerchantId, { status: 'CREATED', currency: 'PLN' })
  expect(oracle.status).toBe(200)
  const refs = (oracle.body?.content ?? []).map(row => row.clientOrderReference)
  expect(refs).toContain(plnRef)
  expect(refs).not.toContain(eurRef)
})

test('PW-M360-E2E-028 applying filters from a stale page query resets to page 0', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'PAGE')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1300, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PAGE'),
  )
  expect(created.status).toBe(201)

  await app.payments.gotoForMerchant(ownedMerchantId, '?page=1')
  await app.payments.expectLoaded()
  const listPath = `/api/merchants/${ownedMerchantId}/payment-orders`
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

test('PW-M360-E2E-025 Amount columnheader sends sort=amountMinor', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'AMT')
  expect((await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'AMT'),
  )).status).toBe(201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  const listPath = `/api/merchants/${ownedMerchantId}/payment-orders`
  const sorted = page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    try {
      const url = new URL(response.url())
      return url.pathname === listPath && (url.searchParams.get('sort') ?? '').includes('amountMinor')
    }
    catch {
      return false
    }
  })
  await app.page.getByRole('button', { name: 'Amount' }).click()
  const response = await sorted
  expect(response.status()).toBe(200)
})

test('PW-M360-E2E-026 status CAPTURED Apply shows captured row', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'CAP')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 2400, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CAP-CREATE'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  const authorized = await client.authorizePayment(
    ownedMerchantId,
    paymentOrderId,
    etagOf(created.headers),
    uniqueIdempotencyKey(testInfo, 'CAP-AUTH'),
  )
  expect(authorized.status).toBe(200)
  const captured = await client.capturePayment(
    ownedMerchantId,
    paymentOrderId,
    etagOf(authorized.headers),
    uniqueIdempotencyKey(testInfo, 'CAP-CAP'),
    2400,
  )
  expect(captured.status).toBe(200)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.applyStatusFilter('Captured')
  await app.payments.expectReferenceVisible(reference)
  await expect(app.page).toHaveURL(/status=CAPTURED/)
  await expect(app.payments.statusBadgeForReference(reference)).toHaveAttribute('data-status', 'CAPTURED')
})

