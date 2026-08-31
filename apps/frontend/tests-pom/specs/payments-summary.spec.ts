import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { mrSummary } from '../methods/metamorphic/SummaryInclusion'
import { etagOf } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { z } from 'zod'

const paymentSummarySchema = z.object({ totalOrders: z.number() }).passthrough()

test('GET summary totalOrders is at least the unfiltered list size (MR-SUMMARY)', async ({ api, ownedMerchantId }, testInfo) => {
  const client = api
  expect(mrSummary.id).toBe('MR-SUMMARY')
  expect((await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1300, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'SUM') },
    uniqueIdempotencyKey(testInfo, 'SUM'),
  )).status).toBe(201)

  const listed = await client.payments.list(ownedMerchantId, { size: 200 })
  expectStatus(listed, 200)
  const summary = await client.payments.summary(ownedMerchantId)
  expectStatus(summary, 200)
  const listSize = listed.body?.totalElements ?? listed.body?.content?.length ?? 0
  expect(summary.body?.totalOrders).toBeGreaterThanOrEqual(listSize)
})

test('payment list summary cards render the total from their BFF response', async ({ app, api, ownedMerchantId, page }, testInfo) => {
  const client = api
  expect((await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1400, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'SUMUI') },
    uniqueIdempotencyKey(testInfo, 'SUMUI'),
  )).status).toBe(201)
  const summaryResponse = waitForBffResponse(page, {
    method: 'GET',
    pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/summary`,
  })
  await app.payments.gotoForMerchant(ownedMerchantId)
  const summary = await summaryResponse
  expect(summary.status()).toBe(200)
  const total = paymentSummarySchema.parse(await summary.json()).totalOrders
  expect(total).toBeGreaterThan(0)

  await app.payments.expectLoaded()
  await expect(app.payments.totalOrdersCard()).toBeVisible()
  await expect(app.payments.totalOrdersValue(total)).toBeVisible()
})

test('GET history grows after authorize then capture', async ({ api, ownedMerchantId }, testInfo) => {
  const client = api
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'HIST') },
    uniqueIdempotencyKey(testInfo, 'HIST'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const before = await client.payments.history(ownedMerchantId, paymentOrderId)
  expectStatus(before, 200)
  const beforeCount = before.body?.content?.length ?? 0

  const detail = await client.payments.get(ownedMerchantId, paymentOrderId)
  const authorized = await client.payments.authorize(
    ownedMerchantId,
    paymentOrderId,
    etagOf(detail.headers),
    uniqueIdempotencyKey(testInfo, 'HIST-A'),
  )
  expectStatus(authorized, 200)
  const captured = await client.payments.capture(
    ownedMerchantId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.payments.get(ownedMerchantId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'HIST-C'),
    1600,
  )
  expectStatus(captured, 200)

  const after = await client.payments.history(ownedMerchantId, paymentOrderId)
  expectStatus(after, 200)
  const entries = after.body?.content ?? []
  expect(entries.length).toBeGreaterThan(beforeCount)
  const toStatuses = entries.map(entry => entry.toStatus)
  expect(toStatuses).toEqual(expect.arrayContaining(['AUTHORIZED', 'CAPTURED']))
})
