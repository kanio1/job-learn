import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { captureAmountPartitions } from '../methods/ep-bva/CaptureAmountPartitions'
import { etagOf, expectProblem } from '../utils/http'

test('capture amount above authorized is 422 (SCN-CAP-OVER)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = api
  const created = await client.payments.createOrder(
    ownedMerchantId,
    {
      amountMinor: captureAmountPartitions.authorizedMinor,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'CAPOVER'),
    },
    uniqueIdempotencyKey(testInfo, 'CAPOVER'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const authorized = await client.payments.authorize(
    ownedMerchantId,
    paymentOrderId,
    etagOf((await client.payments.get(ownedMerchantId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'CAPOVER-A'),
  )
  expectStatus(authorized, 200)
  const captured = await client.payments.capture(
    ownedMerchantId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.payments.get(ownedMerchantId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'CAPOVER-C'),
    captureAmountPartitions.overAmountMinor,
  )
  expect(captured.status).toBe(captureAmountPartitions.expectStatus)
  expectProblem(captured.body, 422, captureAmountPartitions.error)
  const after = await client.payments.get(ownedMerchantId, paymentOrderId)
  expect(after.body?.status).toBe('AUTHORIZED')
})

test('merchant manager expiration sweep is 403', async ({ api }) => {
  const client = api
  const sweep = await client.operations.runExpirationSweep()
  expectStatus(sweep, 403)
  expectProblem(sweep.body, 403)
})
