import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { mrSummary } from '../methods/metamorphic/SummaryInclusion'
import { etagOf } from '../utils/http'

test('GET summary totalOrders is at least the unfiltered list size (MR-SUMMARY)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  expect(mrSummary.id).toBe('MR-SUMMARY')
  expect((await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1300, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'SUM') },
    uniqueIdempotencyKey(testInfo, 'SUM'),
  )).status).toBe(201)

  const listed = await client.listPaymentOrders(ownedMerchantId, { size: 200 })
  expect(listed.status).toBe(200)
  const summary = await client.getPaymentOrdersSummary(ownedMerchantId)
  expect(summary.status).toBe(200)
  const listSize = listed.body?.totalElements ?? listed.body?.content?.length ?? 0
  expect(summary.body?.totalOrders).toBeGreaterThanOrEqual(listSize)
})

test('payment list summary cards match GET summary totalOrders', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  expect((await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1400, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'SUMUI') },
    uniqueIdempotencyKey(testInfo, 'SUMUI'),
  )).status).toBe(201)
  const summary = await client.getPaymentOrdersSummary(ownedMerchantId)
  expect(summary.status).toBe(200)
  const total = summary.body?.totalOrders
  expect(total).toBeGreaterThan(0)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await expect(app.page.getByText('Total orders')).toBeVisible()
  await expect(app.page.getByText('Total orders').locator('..').getByText(String(total), { exact: true })).toBeVisible()
})

test('GET history grows after authorize then capture', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'HIST') },
    uniqueIdempotencyKey(testInfo, 'HIST'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  const before = await client.getPaymentOrderHistory(ownedMerchantId, paymentOrderId)
  expect(before.status).toBe(200)
  const beforeCount = before.body?.content?.length ?? 0

  const detail = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  const authorized = await client.authorizePayment(
    ownedMerchantId,
    paymentOrderId,
    etagOf(detail.headers),
    uniqueIdempotencyKey(testInfo, 'HIST-A'),
  )
  expect(authorized.status).toBe(200)
  const captured = await client.capturePayment(
    ownedMerchantId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.getPaymentOrder(ownedMerchantId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'HIST-C'),
    1600,
  )
  expect(captured.status).toBe(200)

  const after = await client.getPaymentOrderHistory(ownedMerchantId, paymentOrderId)
  expect(after.status).toBe(200)
  const entries = after.body?.content ?? []
  expect(entries.length).toBeGreaterThan(beforeCount)
  const toStatuses = entries.map(entry => entry.toStatus)
  expect(toStatuses).toEqual(expect.arrayContaining(['AUTHORIZED', 'CAPTURED']))
})
