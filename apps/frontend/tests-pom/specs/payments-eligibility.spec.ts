import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { captureAmountPartitions } from '../methods/ep-bva/CaptureAmountPartitions'
import { etagOf, expectProblem } from '../utils/http'

test('capture amount above authorized is 422 (SCN-CAP-OVER)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    {
      amountMinor: captureAmountPartitions.authorizedMinor,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'CAPOVER'),
    },
    uniqueIdempotencyKey(testInfo, 'CAPOVER'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  const authorized = await client.authorizePayment(
    ownedMerchantId,
    paymentOrderId,
    etagOf((await client.getPaymentOrder(ownedMerchantId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'CAPOVER-A'),
  )
  expect(authorized.status).toBe(200)
  const captured = await client.capturePayment(
    ownedMerchantId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.getPaymentOrder(ownedMerchantId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'CAPOVER-C'),
    captureAmountPartitions.overAmountMinor,
  )
  expect(captured.status).toBe(captureAmountPartitions.expectStatus)
  expectProblem(captured.body, 422, captureAmountPartitions.error)
  const after = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  expect(after.body?.status).toBe('AUTHORIZED')
})

test('merchant manager expiration sweep is 403', async ({ api }) => {
  const client = requireApi(api)
  const sweep = await client.runExpirationSweep()
  expect(sweep.status).toBe(403)
  expectProblem(sweep.body, 403)
})
