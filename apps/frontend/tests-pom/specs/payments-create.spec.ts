import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { requestHeader } from '../utils/network'

test('creates a payment order with Idempotency-Key and lands on detail', async ({ app, page }, testInfo) => {
  const reference = uniqueOrderReference(testInfo)
  const idempotencyKey = uniqueIdempotencyKey(testInfo)

  await app.paymentCreate.gotoForMerchant(merchantAlphaId)
  await app.paymentCreate.expectLoaded()
  await app.paymentCreate.fillIdempotencyKey(idempotencyKey)
  await app.paymentCreate.fillAmount(1500)
  await app.paymentCreate.chooseCurrency('PLN')
  await app.paymentCreate.fillReference(reference)

  const createRequest = page.waitForRequest((request) => {
    if (request.method() !== 'POST') {
      return false
    }
    try {
      return new URL(request.url()).pathname === `/api/merchants/${merchantAlphaId}/payment-orders`
    } catch {
      return false
    }
  })
  await app.paymentCreate.submit()
  const posted = await createRequest
  expect(requestHeader(posted, 'Idempotency-Key')).toBe(idempotencyKey)

  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('payment-order-detail').getByText(reference)).toBeVisible()
  await expect(app.page).toHaveURL(new RegExp(`/admin/merchants/${merchantAlphaId}/payments/[0-9a-f-]{36}`))
})

test('replaying the same Idempotency-Key returns the same order; mismatch is 409', async ({ api }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'IDEM')
  const key = uniqueIdempotencyKey(testInfo, 'REPLAY')
  const payload = { amountMinor: 1700, currency: 'PLN' as const, clientOrderReference: reference }

  const first = await api.createPaymentOrder(merchantAlphaId, payload, key)
  expect(first.status).toBe(201)
  expect(first.body.paymentOrderId).toBeTruthy()

  const replay = await api.createPaymentOrder(merchantAlphaId, payload, key)
  expect(replay.status).toBe(200)
  expect(replay.body.paymentOrderId).toBe(first.body.paymentOrderId)

  const conflict = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 9999, currency: 'EUR', clientOrderReference: `${reference}-X` },
    key,
  )
  expect(conflict.status).toBe(409)
  expect(conflict.body.error).toBe('idempotency_conflict')
})
