import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { waitForBffResponse } from '../utils/wait-bff'

test('manual refresh GETs the live order and then shows Authorized after API authorize', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'POLL') },
    uniqueIdempotencyKey(testInfo, 'POLL'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()
  const detailPath = `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}`

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.currentStatus()).toHaveAttribute('data-status', 'CREATED')

  const refresh = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
  await app.paymentDetail.refreshStatus()
  const refreshed = await refresh
  expect(refreshed.status()).toBe(200)
  expect(refreshed.headers()['etag']).toBeTruthy()
  expect(refreshed.headers()['cache-control'] ?? '').toMatch(/no-store/i)
  await expect(app.paymentDetail.currentStatus()).toHaveAttribute('data-status', 'CREATED')

  const etag = refreshed.headers()['etag'] ?? created.headers['etag'] ?? ''
  const authorized = await client.authorizePayment(
    ownedMerchantId,
    paymentOrderId!,
    etag,
    uniqueIdempotencyKey(testInfo, 'POLLAUTH'),
  )
  expectStatus(authorized, 200)

  const afterAuthorize = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
  await app.paymentDetail.refreshStatus()
  expect((await afterAuthorize).status()).toBe(200)
  await expect(app.paymentDetail.currentStatus()).toHaveAttribute('data-status', 'AUTHORIZED')
})

test('auto refresh issues a live GET without a mocked status flip', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1800, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'AUTOPOLL') },
    uniqueIdempotencyKey(testInfo, 'AUTOPOLL'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()
  const detailPath = `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}`

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId!)
  await app.paymentDetail.expectLoaded()

  const polled = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
  await app.paymentDetail.enableAutoRefresh()
  expect((await polled).status()).toBe(200)
  await expect(app.paymentDetail.currentStatus()).toHaveAttribute('data-status', 'CREATED')
})
