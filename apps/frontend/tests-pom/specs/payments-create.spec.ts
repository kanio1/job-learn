import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectProblem, headerOf, locationOf } from '../utils/http'
import { requestHeader } from '../utils/network'
import { waitForBffRequest } from '../utils/wait-bff'
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
  await app.paymentCreate.chooseCurrency('PLN')
  await app.paymentCreate.fillReference(reference)

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
  expect(first.status).toBe(201)
  expect(first.body.paymentOrderId).toBeTruthy()
  expect(locationOf(first.headers) ?? '').toMatch(/\/payment-orders\//)
  expect(headerOf(first.headers, 'idempotency-replayed') ?? 'false').toBe('false')

  const replay = await client.createPaymentOrder(ownedMerchantId, payload, key)
  expect(replay.status).toBe(200)
  expect(replay.body.paymentOrderId).toBe(first.body.paymentOrderId)
  expect(headerOf(replay.headers, 'idempotency-replayed')).toBe('true')
  expect(headerOf(replay.headers, 'etag')).toBe(headerOf(first.headers, 'etag'))

  const persisted = await client.getPaymentOrder(ownedMerchantId, first.body.paymentOrderId!)
  expect(persisted.status).toBe(200)
  expect(persisted.body?.clientOrderReference).toBe(reference)

  const listed = await client.listPaymentOrders(ownedMerchantId, {
    clientOrderReference: reference,
    page: 0,
    size: 20,
  })
  expect(listed.status).toBe(200)
  expect(listed.body?.totalElements).toBe(1)
  expect((listed.body?.content ?? []).map(row => row.paymentOrderId)).toEqual([first.body.paymentOrderId])

  const conflict = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 9999, currency: 'EUR', clientOrderReference: `${reference}-X` },
    key,
  )
  expect(conflict.status).toBe(409)
  expectProblem(conflict.body, 409, 'idempotency_conflict')
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
      expect(created.status, row.id).toBe(row.expectStatus)
      if (row.expectStatus === 400) {
        expectProblem(created.body, 400)
        return
      }
      expect(created.body?.paymentOrderId, row.id).toBeTruthy()
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
      expect(created.status, row.id).toBe(row.expectStatus)
      if (row.expectStatus === 400) {
        expectProblem(created.body, 400)
        return
      }
      expect(created.body?.paymentOrderId, row.id).toBeTruthy()
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
    expect(created.status, row.id).toBe(row.expectStatus)
    expect(created.body.paymentOrderId, row.id).toBeTruthy()
    ids.push(created.body.paymentOrderId!)
  }
  expect(new Set(ids).size).toBe(ids.length)
})
