import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'

test('status USelect is not a native select and list badge carries data-status', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'BADGE')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1400, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'BADGE'),
  )
  expectStatus(created, 201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await expect(app.page.locator('select[name="status"], select#status')).toHaveCount(0)
  await app.payments.applyStatusFilter('Created')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await app.payments.filterByClientReference(reference)
  await app.payments.expectReferenceVisible(reference)
  await expect(app.payments.statusBadgeForReference(reference)).toBeVisible()
  await expect(app.payments.statusBadgeForReference(reference)).toHaveAttribute('data-status', 'CREATED')
  await expect(app.payments.statusBadgeForReference(reference)).toContainText('Created')

  await app.paymentDetail.gotoOrder(ownedMerchantId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.statusInDetail('Created')).toBeVisible()
})

test('dismissing ConfirmModal does not cancel the payment', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'DISC')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'DISC'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  let cancelPosted = false
  const onRequest = (request: { method: () => string, url: () => string }) => {
    if (request.method() === 'POST' && request.url().includes(`/payment-orders/${paymentOrderId}/cancel`)) {
      cancelPosted = true
    }
  }
  page.on('request', onRequest)
  try {
    await app.paymentDetail.openCancelThenDismiss()
    await expect(app.page.getByRole('heading', { name: /Confirm Cancel/ })).toHaveCount(0)
    await expect(app.paymentDetail.statusInDetail('Created')).toBeVisible()
    expect(cancelPosted).toBe(false)
  }
  finally {
    page.off('request', onRequest)
  }

  const stillCreated = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')
})
