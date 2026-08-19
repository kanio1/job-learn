import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { mrIdem, mrUniq } from '../methods/metamorphic/IdempotentIdentity'
import { mrEtag } from '../methods/metamorphic/EtagStability'
import { metamorphicListFilter } from '../methods/combinations/MetamorphicListFilter'

test('MR-IDEM replay keeps the same paymentOrderId; MR-UNIQ issues two ids', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const payload = {
    amountMinor: 2100,
    currency: 'PLN',
    clientOrderReference: uniqueOrderReference(testInfo, mrIdem.id),
  }
  const key = uniqueIdempotencyKey(testInfo, mrIdem.id)
  const first = await client.createPaymentOrder(ownedMerchantId, payload, key)
  expect(first.status).toBe(201)
  const replay = await client.createPaymentOrder(ownedMerchantId, payload, key)
  expect(replay.status).toBe(mrIdem.replayStatus)
  expect(replay.body.paymentOrderId).toBe(first.body.paymentOrderId)

  const second = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, mrUniq.id) },
    uniqueIdempotencyKey(testInfo, mrUniq.id),
  )
  expect(second.status).toBe(mrUniq.secondStatus)
  expect(second.body.paymentOrderId).not.toBe(first.body.paymentOrderId)
})

test('MR-ETAG is stable across GET then changes after authorize', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 1800, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, mrEtag.id) },
    uniqueIdempotencyKey(testInfo, mrEtag.id),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  const first = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  const second = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  expect(first.headers['etag']).toBeTruthy()
  expect(second.headers['etag']).toBe(first.headers['etag'])

  const authorized = await client.authorizePayment(
    ownedMerchantId, paymentOrderId, first.headers['etag']!, uniqueIdempotencyKey(testInfo, `${mrEtag.id}-A`),
  )
  expect(authorized.status).toBe(200)
  const after = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  expect(after.headers['etag']).toBeTruthy()
  expect(after.headers['etag']).not.toBe(first.headers['etag'])
})

test('MR-FILTER: narrower minAmount results are included in the wider list', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const highRef = uniqueOrderReference(testInfo, 'MRHI')
  const lowRef = uniqueOrderReference(testInfo, 'MRLO')
  expect((await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: metamorphicListFilter.highAmount, currency: 'PLN', clientOrderReference: highRef },
    uniqueIdempotencyKey(testInfo, 'MRHI'),
  )).status).toBe(201)
  expect((await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: metamorphicListFilter.lowAmount, currency: 'PLN', clientOrderReference: lowRef },
    uniqueIdempotencyKey(testInfo, 'MRLO'),
  )).status).toBe(201)

  const narrow = await client.listPaymentOrders(ownedMerchantId, {
    minAmount: metamorphicListFilter.narrowMin,
    size: 200,
  })
  const wide = await client.listPaymentOrders(ownedMerchantId, {
    minAmount: metamorphicListFilter.wideMin,
    size: 200,
  })
  expect(narrow.status).toBe(200)
  expect(wide.status).toBe(200)
  const narrowRefs = (narrow.body?.content ?? []).map(row => row.clientOrderReference)
  const wideRefs = (wide.body?.content ?? []).map(row => row.clientOrderReference)
  expect(narrowRefs).toContain(highRef)
  expect(narrowRefs).not.toContain(lowRef)
  expect(wideRefs).toContain(highRef)
  expect(wideRefs).toContain(lowRef)
  for (const ref of narrowRefs) {
    expect(wideRefs, `MR-FILTER ${ref}`).toContain(ref)
  }
})


