import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { observeRequests } from '../utils/network-observer'
import { expectStatus } from '../api/bff-client'

test('status USelect is not a native select and list badge carries data-status', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'BADGE')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1400, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'BADGE'),
  )
  expectStatus(created, 201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await expect(app.payments.filters.nativeStatusSelect()).toHaveCount(0)
  await app.payments.filters.applyStatus('Created')
  await expect(app.page).toHaveURL(/status=CREATED/)
  await app.payments.filters.applyClientReference(reference)
  await expect(app.payments.referenceInTable(reference)).toBeVisible()
  await expect(app.payments.statusBadgeForReference(reference)).toBeVisible()
  await expect(app.payments.statusBadgeForReference(reference)).toHaveAttribute('data-status', 'CREATED')
  await expect(app.payments.statusBadgeForReference(reference)).toContainText('Created')

  await app.paymentDetail.gotoOrder(ownedMerchantId, created.body.paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.statusInDetail('Created')).toBeVisible()
})

test('dismissing ConfirmModal does not cancel the payment', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'DISC')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'DISC'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  const { requests } = await observeRequests(page,
    request => request.method() === 'POST' && request.url().includes(`/payment-orders/${paymentOrderId}/cancel`),
    async () => {
    await app.paymentDetail.openCancelThenDismiss()
    await expect(app.paymentDetail.confirm.heading(/Confirm Cancel/)).toHaveCount(0)
    await expect(app.paymentDetail.statusInDetail('Created')).toBeVisible()
    },
  )
  expect(requests).toHaveLength(0)

  const stillCreated = await client.payments.get(ownedMerchantId, paymentOrderId)
  expect(stillCreated.body?.status).toBe('CREATED')
})
