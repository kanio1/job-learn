import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { etagOf, expectNoAuthTokenLeak, expectProblem, headerOf } from '../utils/http'
import { ifMatchPatchMatrix } from '../methods/decision-table/IfMatchActionMatrix'

test('GET If-None-Match is 304 empty; HEAD is 200 empty with ETag', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    {
      amountMinor: 1800,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'COND'),
    },
    uniqueIdempotencyKey(testInfo, 'COND'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()

  const initial = await client.getPaymentOrder(ownedMerchantId, paymentOrderId!)
  expect(initial.status).toBe(200)
  const etag = etagOf(initial.headers)
  expect(etag, 'GET detail must return ETag').toBeTruthy()
  expect(headerOf(initial.headers, 'cache-control') ?? '').toMatch(/no-store/i)
  expect(headerOf(initial.headers, 'cache-control') ?? '').toMatch(/no-transform/i)
  expect(headerOf(initial.headers, 'vary') ?? '').toMatch(/authorization/i)

  const conditional = await client.getPaymentOrder(ownedMerchantId, paymentOrderId!, {
    'If-None-Match': etag!,
  })
  expect(conditional.status).toBe(304)
  expect(conditional.raw ?? '').toBe('')
  expect(etagOf(conditional.headers)).toBe(etag)
  expect(headerOf(conditional.headers, 'cache-control') ?? '').toMatch(/no-store/i)
  expect(headerOf(conditional.headers, 'cache-control') ?? '').toMatch(/no-transform/i)
  expect(headerOf(conditional.headers, 'vary') ?? '').toMatch(/authorization/i)

  const head = await client.headPaymentOrder(ownedMerchantId, paymentOrderId!)
  expect(head.status).toBe(200)
  expect(head.raw).toBe('')
  expect(etagOf(head.headers)).toBeTruthy()
  expect(headerOf(head.headers, 'cache-control') ?? '').toMatch(/no-transform/i)
  expectNoAuthTokenLeak(head.headers, head.raw)
})

test('PATCH metadata without If-Match is 428; stale If-Match is 412; fresh If-Match is 200', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  expect(ifMatchPatchMatrix.map(row => row.id)).toEqual(['SCN-IFM-05', 'SCN-IFM-06'])
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    {
      amountMinor: 1900,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'P428'),
    },
    uniqueIdempotencyKey(testInfo, 'P428'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  const missing = await client.patchPaymentOrder(
    ownedMerchantId,
    paymentOrderId,
    { metadata: { note: 'pom-rest' } },
  )
  expect(missing.status).toBe(428)
  expectProblem(missing.body, 428)

  const stale = await client.patchPaymentOrder(
    ownedMerchantId,
    paymentOrderId,
    { metadata: { note: 'pom-stale' } },
    '"v99"',
  )
  expect(stale.status).toBe(412)
  expectProblem(stale.body, 412)

  const before = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  const etag = etagOf(before.headers)
  expect(etag).toBeTruthy()
  const patched = await client.patchPaymentOrder(
    ownedMerchantId,
    paymentOrderId,
    { metadata: { note: 'pom-ok' } },
    etag,
  )
  expect(patched.status).toBe(200)
  const after = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
  expect(etagOf(after.headers)).toBeTruthy()
  expect(etagOf(after.headers)).not.toBe(etag)
})
