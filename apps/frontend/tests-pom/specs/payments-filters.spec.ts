import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { utcToday } from '../utils/dates'
import { etagOf } from '../utils/http'

test('list filters by amount with API oracle and BFF composition', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = api
  const lowRef = uniqueOrderReference(testInfo, 'LOW')
  const highRef = uniqueOrderReference(testInfo, 'HIGH')
  const low = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1100, currency: 'PLN', clientOrderReference: lowRef },
    uniqueIdempotencyKey(testInfo, 'LOW'),
  )
  const high = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 8800, currency: 'PLN', clientOrderReference: highRef },
    uniqueIdempotencyKey(testInfo, 'HIGH'),
  )
  expectStatus(low, 201)
  expectStatus(high, 201)

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

  await app.payments.filters.applyAmountRange(5000, 20000)
  await expect(app.payments.referenceInTable(highRef)).toBeVisible()
  await expect(app.payments.referenceInTable(lowRef)).toHaveCount(0)

  const oracle = await client.payments.list(ownedMerchantId, { minAmount: 5000, maxAmount: 20000 })
  expectStatus(oracle, 200)
  const refs = (oracle.body?.content ?? []).map(row => row.clientOrderReference)
  expect(refs).toContain(highRef)
  expect(refs).not.toContain(lowRef)
  await expectNoTokenInBrowserStorage(app.page)
})

test('PW-M360-E2E-027 date status and reference filters agree with API oracle', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'PAIR')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 2500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PAIR'),
  )
  expectStatus(created, 201)
  const today = utcToday()

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyDateRange(today, today)
  await app.payments.filters.applyStatus('Created')
  await app.payments.filters.applyClientReference(reference)
  await expect(app.page).toHaveURL(new RegExp(`status=CREATED`))
  await expect(app.page).toHaveURL(new RegExp(`clientOrderReference=${reference}`))
  await expect(app.payments.referenceInTable(reference)).toBeVisible()

  const oracle = await client.payments.list(ownedMerchantId, {
    fromDate: today,
    toDate: today,
    status: 'CREATED',
    clientOrderReference: reference,
  })
  expectStatus(oracle, 200)
  expect(oracle.body?.content?.some(row => row.clientOrderReference === reference)).toBe(true)
})

test('status and currency pairwise filter agrees with API oracle', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const plnRef = uniqueOrderReference(testInfo, 'PLN')
  const eurRef = uniqueOrderReference(testInfo, 'EUR')
  const pln = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: plnRef },
    uniqueIdempotencyKey(testInfo, 'PLN'),
  )
  const eur = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 2200, currency: 'EUR', clientOrderReference: eurRef },
    uniqueIdempotencyKey(testInfo, 'EUR'),
  )
  expectStatus(pln, 201)
  expectStatus(eur, 201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyStatus('Created')
  await app.payments.filters.applyCurrency('PLN')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await expect(app.page).toHaveURL(/currency=PLN/)
  await expect(app.payments.referenceInTable(plnRef)).toBeVisible()
  await expect(app.payments.referenceInTable(eurRef)).toHaveCount(0)

  const oracle = await client.payments.list(ownedMerchantId, { status: 'CREATED', currency: 'PLN' })
  expectStatus(oracle, 200)
  const refs = (oracle.body?.content ?? []).map(row => row.clientOrderReference)
  expect(refs).toContain(plnRef)
  expect(refs).not.toContain(eurRef)
})

test('PW-M360-E2E-028 applying filters from a stale page query resets to page 0', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'PAGE')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1300, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PAGE'),
  )
  expectStatus(created, 201)

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
  await app.payments.filters.applyClientReference(reference)
  const seen = await reset
  const pageParam = new URL(seen.url()).searchParams.get('page')
  expect(pageParam === null || pageParam === '0').toBe(true)
  await expect(app.page).not.toHaveURL(/page=1/)
  await expect(app.payments.referenceInTable(reference)).toBeVisible()
})

test('PW-M360-E2E-025 Amount columnheader sends sort=amountMinor', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'AMT')
  expect((await client.payments.createOrder(
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
  await app.payments.amountColumnButton().click()
  const response = await sorted
  expect(response.status()).toBe(200)
})

test('PW-M360-E2E-026 status CAPTURED Apply shows captured row', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'CAP')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 2400, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CAP-CREATE'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const authorized = await client.payments.authorize(
    ownedMerchantId,
    paymentOrderId,
    etagOf(created.headers),
    uniqueIdempotencyKey(testInfo, 'CAP-AUTH'),
  )
  expectStatus(authorized, 200)
  const captured = await client.payments.capture(
    ownedMerchantId,
    paymentOrderId,
    etagOf(authorized.headers),
    uniqueIdempotencyKey(testInfo, 'CAP-CAP'),
    2400,
  )
  expectStatus(captured, 200)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyStatus('Captured')
  await expect(app.payments.referenceInTable(reference)).toBeVisible()
  await expect(app.page).toHaveURL(/status=CAPTURED/)
  await expect(app.payments.statusBadgeForReference(reference)).toHaveAttribute('data-status', 'CAPTURED')
})
