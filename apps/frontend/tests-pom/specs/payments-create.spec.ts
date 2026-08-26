import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { correlationIdOf, expectNoAuthTokenLeak, expectProblem, headerOf, locationOf } from '../utils/http'
import { requestHeader } from '../utils/network'
import { waitForBffRequest, waitForBffResponse } from '../utils/wait-bff'
import { createUcEpRest } from '../methods/combinations/CreateUcEpRest'
import { orderReferenceFor } from '../methods/ep-bva/OrderReferencePartitions'

test('creates a payment order with Idempotency-Key and lands on detail', async ({ app, page, ownedMerchantId }, testInfo) => {
  expect(createUcEpRest.journey.expectStatus).toBe(201)
  const reference = uniqueOrderReference(testInfo)
  const idempotencyKey = uniqueIdempotencyKey(testInfo)

  await app.paymentCreate.gotoForMerchant(ownedMerchantId)
  await app.paymentCreate.expectLoaded()
  await app.paymentCreate.fillIdempotencyKey(idempotencyKey)
  await app.paymentCreate.fillAmount(1500)
  await app.paymentCreate.next()
  await app.paymentCreate.chooseCurrency('PLN')
  await app.paymentCreate.next()
  await app.paymentCreate.fillReference(reference)
  await app.paymentCreate.next()

  const createRequest = waitForBffRequest(page, {
    method: 'POST',
    pathExact: `/api/merchants/${ownedMerchantId}/payment-orders`,
  })
  await app.paymentCreate.submit()
  const posted = await createRequest
  expect(requestHeader(posted, 'Idempotency-Key')).toBe(idempotencyKey)

  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('payment-order-detail').getByText(reference)).toBeVisible()
  await expect(app.page).toHaveURL(new RegExp(`/admin/merchants/${ownedMerchantId}/payments/[0-9a-f-]{36}`))
})

test('replaying the same Idempotency-Key returns the same order; mismatch is 409', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'IDEM')
  const key = uniqueIdempotencyKey(testInfo, 'REPLAY')
  const payload = { amountMinor: 1700, currency: 'PLN' as const, clientOrderReference: reference }

  const first = await client.createPaymentOrder(ownedMerchantId, payload, key)
  expectStatus(first, 201)
  expect(first.body.paymentOrderId).toBeTruthy()
  expect(locationOf(first.headers) ?? '').toMatch(/\/payment-orders\//)
  expect(headerOf(first.headers, 'idempotency-replayed') ?? 'false').toBe('false')

  const replay = await client.createPaymentOrder(ownedMerchantId, payload, key)
  expectStatus(replay, 200)
  expect(replay.body.paymentOrderId).toBe(first.body.paymentOrderId)
  expect(headerOf(replay.headers, 'idempotency-replayed')).toBe('true')
  expect(headerOf(replay.headers, 'etag')).toBe(headerOf(first.headers, 'etag'))

  const persisted = await client.getPaymentOrder(ownedMerchantId, first.body.paymentOrderId!)
  expectStatus(persisted, 200)
  expect(persisted.body?.clientOrderReference).toBe(reference)

  const listed = await client.listPaymentOrders(ownedMerchantId, {
    clientOrderReference: reference,
    page: 0,
    size: 20,
  })
  expectStatus(listed, 200)
  expect(listed.body?.totalElements).toBe(1)
  expect((listed.body?.content ?? []).map(row => row.paymentOrderId)).toEqual([first.body.paymentOrderId])

  const conflict = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 9999, currency: 'EUR', clientOrderReference: `${reference}-X` },
    key,
  )
  expectStatus(conflict, 409)
  expectProblem(conflict.body, 409, 'idempotency_conflict')
})

test('BFF forwards a caller correlation id on create without exposing session material', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const correlationId = `pom-correlation-${testInfo.workerIndex}-${testInfo.testId}`
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'CORR') },
    uniqueIdempotencyKey(testInfo, 'CORR'),
    correlationId,
  )
  expectStatus(created, 201)
  expect(correlationIdOf(created.headers)).toBe(correlationId)
  expectNoAuthTokenLeak(created.headers, JSON.stringify(created.body))
})

test('create-order amount BVA/EP partitions stay on BFF REST (SCN-PAY-06..11)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  for (const row of createUcEpRest.amountPartitions) {
    await test.step(row.id, async () => {
      const created = await client.createPaymentOrder(
        ownedMerchantId,
        {
          amountMinor: row.amountMinor,
          currency: row.currency,
          clientOrderReference: uniqueOrderReference(testInfo, row.id),
        },
        uniqueIdempotencyKey(testInfo, row.id),
      )
      if (row.expectStatus === 400) {
        expect(created.status, row.id).toBe(400)
        expectProblem(created.body, 400)
        return
      }
      expectStatus(created, 201, row.id)
      expect(created.body.paymentOrderId, row.id).toBeTruthy()
    })
  }
})

test('create-order reference length BVA stays on BFF REST (SCN-PAY-12..15)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  for (const row of createUcEpRest.orderReferencePartitions) {
    await test.step(row.id, async () => {
      const created = await client.createPaymentOrder(
        ownedMerchantId,
        {
          amountMinor: 1500,
          currency: 'PLN',
          clientOrderReference: orderReferenceFor(row.kind, uniqueOrderReference(testInfo, row.id)),
        },
        uniqueIdempotencyKey(testInfo, row.id),
      )
      if (row.expectStatus === 400) {
        expect(created.status, row.id).toBe(400)
        expectProblem(created.body, 400)
        return
      }
      expectStatus(created, 201, row.id)
      expect(created.body.paymentOrderId, row.id).toBeTruthy()
    })
  }
})

test('BFF mints a key when Idempotency-Key is missing or empty (SCN-PAY-04/05)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const ids: string[] = []
  for (const row of createUcEpRest.idempotencyKeyBoundaries) {
    const key = row.keyMode === 'empty' ? '' : undefined
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      {
        amountMinor: 1500,
        currency: 'PLN',
        clientOrderReference: uniqueOrderReference(testInfo, row.id),
      },
      key,
    )
    expect(row.expectStatus, row.id).toBe(201)
    expectStatus(created, 201, row.id)
    expect(created.body.paymentOrderId, row.id).toBeTruthy()
    ids.push(created.body.paymentOrderId!)
  }
  expect(new Set(ids).size).toBe(ids.length)
})

test('PW-M360-E2E-134 Next without amount shows field error', async ({ app, ownedMerchantId }) => {
  await app.paymentCreate.gotoForMerchant(ownedMerchantId)
  await app.paymentCreate.expectLoaded()
  await app.paymentCreate.next()
  await expect(app.page.getByText('Amount must be at least 1')).toBeVisible()
})

test('PW-M360-E2E-135 full wizard POSTs create 201', async ({ app, page, ownedMerchantId }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'WIZ')
  const idempotencyKey = uniqueIdempotencyKey(testInfo, 'WIZ')
  await app.paymentCreate.gotoForMerchant(ownedMerchantId)
  await app.paymentCreate.expectLoaded()
  await app.paymentCreate.fillIdempotencyKey(idempotencyKey)
  await app.paymentCreate.fillAmount(1800)
  await app.paymentCreate.next()
  await app.paymentCreate.chooseCurrency('EUR')
  await app.paymentCreate.next()
  await app.paymentCreate.fillReference(reference)
  await app.paymentCreate.next()
  await expect(app.page.getByTestId('create-payment-order-review')).toContainText(reference)

  const created = waitForBffResponse(page, {
    method: 'POST',
    pathExact: `/api/merchants/${ownedMerchantId}/payment-orders`,
  })
  await app.paymentCreate.submit()
  expect((await created).status()).toBe(201)
  await app.paymentDetail.expectLoaded()
})

test('PW-M360-E2E-136 double click Create is 201 then replay 200', async ({ app, page, ownedMerchantId }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'DBL')
  const idempotencyKey = uniqueIdempotencyKey(testInfo, 'DBL')
  await app.paymentCreate.gotoForMerchant(ownedMerchantId)
  await app.paymentCreate.expectLoaded()
  await app.paymentCreate.fillIdempotencyKey(idempotencyKey)
  await app.paymentCreate.fillAmount(1900)
  await app.paymentCreate.next()
  await app.paymentCreate.chooseCurrency('USD')
  await app.paymentCreate.next()
  await app.paymentCreate.fillReference(reference)
  await app.paymentCreate.next()

  const statuses: number[] = []
  const createPath = `/api/merchants/${ownedMerchantId}/payment-orders`
  page.on('response', (response) => {
    try {
      if (response.request().method() === 'POST' && new URL(response.url()).pathname === createPath) {
        statuses.push(response.status())
      }
    }
    catch {
      // ignore
    }
  })
  await app.page.getByTestId('action-create-payment-order').dblclick()
  await expect.poll(() => statuses.slice().sort((a, b) => a - b)).toEqual([200, 201])
})
