import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { illegalStDtRest, headerStDtRest } from '../methods/combinations/IllegalStDt'
import { authorizeHeaderBoundaries } from '../methods/ep-bva/IfMatchAndKeyBoundaries'
import { ifMatchActionMatrix } from '../methods/decision-table/IfMatchActionMatrix'
import type { BffClient } from '../api/bff-client'
import { expectProblem } from '../utils/http'
import type { PaymentStatus } from '../methods/state/PaymentStatusMachine'

async function createCreated(
  client: BffClient,
  merchantId: string,
  testInfo: { titlePath: string[] },
  tag: string,
) {
  const created = await client.createPaymentOrder(
    merchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, tag) },
    uniqueIdempotencyKey(testInfo, tag),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()
  return paymentOrderId!
}

async function etag(client: BffClient, merchantId: string, paymentOrderId: string) {
  const get = await client.getPaymentOrder(merchantId, paymentOrderId)
  expect(get.status).toBe(200)
  const value = get.headers['etag']
  expect(value).toBeTruthy()
  return value!
}

async function bringTo(
  client: BffClient,
  merchantId: string,
  paymentOrderId: string,
  testInfo: { titlePath: string[] },
  target: PaymentStatus,
) {
  if (target === 'CREATED') {
    return
  }
  const fresh = await etag(client, merchantId, paymentOrderId)
  if (target === 'CANCELLED') {
    const cancelled = await client.cancelPayment(
      merchantId, paymentOrderId, fresh, uniqueIdempotencyKey(testInfo, 'TO-CAN'),
    )
    expect(cancelled.status).toBe(200)
    return
  }
  const authorized = await client.authorizePayment(
    merchantId, paymentOrderId, fresh, uniqueIdempotencyKey(testInfo, 'TO-AUTH'),
  )
  expect(authorized.status).toBe(200)
  if (target === 'AUTHORIZED') {
    return
  }
  const afterAuth = await etag(client, merchantId, paymentOrderId)
  const captured = await client.capturePayment(
    merchantId, paymentOrderId, afterAuth, uniqueIdempotencyKey(testInfo, 'TO-CAP'), 2100,
  )
  expect(captured.status).toBe(200)
}

test('illegal lifecycle edges stay on BFF REST (SCN-ILL)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  expect(illegalStDtRest.map(row => row.id)).toEqual(
    expect.arrayContaining(['SCN-ILL-01', 'SCN-ILL-02', 'SCN-ILL-03', 'SCN-ILL-04', 'SCN-ILL-05']),
  )
  for (const row of illegalStDtRest) {
    const paymentOrderId = await createCreated(client, ownedMerchantId, testInfo, row.id)
    await bringTo(client, ownedMerchantId, paymentOrderId, testInfo, row.from)
    const match = await etag(client, ownedMerchantId, paymentOrderId)
    const key = uniqueIdempotencyKey(testInfo, `${row.id}-ACT`)
    const result = row.action === 'capture'
      ? await client.capturePayment(ownedMerchantId, paymentOrderId, match, key, 2100)
      : row.action === 'refund'
        ? await client.refundPayment(ownedMerchantId, paymentOrderId, match, key)
        : row.action === 'cancel'
          ? await client.cancelPayment(ownedMerchantId, paymentOrderId, match, key)
          : await client.authorizePayment(ownedMerchantId, paymentOrderId, match, key)
    expect(result.status, row.id).toBe(row.expectStatus)
    expectProblem(result.body, row.expectStatus)
    const after = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    expect(after.body?.status, row.id).toBe(row.to)
  }
})

test('If-Match BVA on authorize is REST (absent 428, v99 412, malformed 400)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  expect(headerStDtRest.length).toBeGreaterThan(0)
  for (const row of authorizeHeaderBoundaries.filter(item => item.ifMatch !== 'fresh')) {
    const paymentOrderId = await createCreated(client, ownedMerchantId, testInfo, row.id)
    const fresh = await etag(client, ownedMerchantId, paymentOrderId)
    const ifMatch = row.ifMatch === 'absent' ? undefined : row.ifMatch === 'v99' ? '"v99"' : 'stale-etag'
    const result = await client.authorizePayment(
      ownedMerchantId, paymentOrderId, ifMatch, uniqueIdempotencyKey(testInfo, row.id),
    )
    expect(result.status, row.id).toBe(row.expectStatus)
    expectProblem(result.body, row.expectStatus)
    const after = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    expect(after.body?.status).toBe('CREATED')
    expect(fresh).toBeTruthy()
  }
})

test('If-Match × action DT stays on BFF REST (SCN-IFM)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  for (const row of ifMatchActionMatrix) {
    const paymentOrderId = await createCreated(client, ownedMerchantId, testInfo, row.id)
    await bringTo(client, ownedMerchantId, paymentOrderId, testInfo, row.from)
    const ifMatch = row.ifMatch === 'absent' ? undefined : row.ifMatch === 'v99' ? '"v99"' : 'stale-etag'
    const key = uniqueIdempotencyKey(testInfo, `${row.id}-ACT`)
    const result = row.action === 'capture'
      ? await client.capturePayment(ownedMerchantId, paymentOrderId, ifMatch, key, 2100)
      : await client.cancelPayment(ownedMerchantId, paymentOrderId, ifMatch, key)
    expect(result.status, row.id).toBe(row.expectStatus)
    expectProblem(result.body, row.expectStatus)
    const after = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    expect(after.body?.status, row.id).toBe(row.to)
  }
})
