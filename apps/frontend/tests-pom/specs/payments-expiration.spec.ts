import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { etagOf } from '../utils/http'

/**
 * Live authorization window display. Server expiry (sweep / lazy capture) is
 * Java-only — this spec does not page.clock a mocked expiresAt and does not
 * wait for the scheduler.
 */
test('CREATED has no countdown; AUTHORIZED shows a live expiresAt countdown', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const created = await client.payments.createOrder(
    ownedMerchantId,
    {
      amountMinor: 2200,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'EXP'),
    },
    uniqueIdempotencyKey(testInfo, 'EXP'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.expirationCountdown()).toHaveCount(0)

  const before = await client.payments.get(ownedMerchantId, paymentOrderId!)
  const authorized = await client.payments.authorize(
    ownedMerchantId,
    paymentOrderId!,
    etagOf(before.headers),
    uniqueIdempotencyKey(testInfo, 'EXPAUTH'),
  )
  expectStatus(authorized, 200)

  await app.paymentDetail.refreshStatus()
  await expect(app.paymentDetail.statusInDetail('Authorized')).toBeVisible()
  const after = await client.payments.get(ownedMerchantId, paymentOrderId!)
  expect(after.body?.expiresAt).toBeTruthy()
  await expect(app.paymentDetail.expirationCountdown()).toBeVisible()
  await expect(app.paymentDetail.expirationCountdownRemaining()).toBeVisible()
})
