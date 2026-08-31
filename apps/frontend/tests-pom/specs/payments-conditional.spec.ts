import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { expectEmpty } from '../api/contracts/http-result'
import { etagOf, expectNoAuthTokenLeak, expectProblem, headerOf } from '../utils/http'
import { ifMatchPatchMatrix } from '../methods/decision-table/IfMatchActionMatrix'

test('GET If-None-Match is 304 empty; HEAD is 200 empty with ETag', async ({ api, ownedMerchantId }, testInfo) => {
  const client = api
  const created = await client.payments.createOrder(
    ownedMerchantId,
    {
      amountMinor: 1800,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'COND'),
    },
    uniqueIdempotencyKey(testInfo, 'COND'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()

  const initial = await client.payments.get(ownedMerchantId, paymentOrderId!)
  expectStatus(initial, 200)
  const etag = etagOf(initial.headers)
  expect(etag, 'GET detail must return ETag').toBeTruthy()
  expect(headerOf(initial.headers, 'cache-control') ?? '').toMatch(/no-store/i)
  expect(headerOf(initial.headers, 'cache-control') ?? '').toMatch(/no-transform/i)
  expect(headerOf(initial.headers, 'vary') ?? '').toMatch(/authorization/i)

  const conditional = await client.payments.get(ownedMerchantId, paymentOrderId!, {
    'If-None-Match': etag!,
  })
  const emptyConditional = expectEmpty(conditional, 304)
  expect(etagOf(emptyConditional.headers)).toBe(etag)
  expect(headerOf(emptyConditional.headers, 'cache-control') ?? '').toMatch(/no-store/i)
  expect(headerOf(emptyConditional.headers, 'cache-control') ?? '').toMatch(/no-transform/i)
  expect(headerOf(emptyConditional.headers, 'vary') ?? '').toMatch(/authorization/i)

  const head = await client.payments.head(ownedMerchantId, paymentOrderId!)
  expect(head.kind).toBe('empty')
  expect(etagOf(head.headers)).toBeTruthy()
  expect(headerOf(head.headers, 'cache-control') ?? '').toMatch(/no-transform/i)
  expectNoAuthTokenLeak(head.headers, '')
})

test('PATCH metadata without If-Match is 428; stale If-Match is 412; fresh If-Match is 200', async ({ api, ownedMerchantId }, testInfo) => {
  const client = api
  expect(ifMatchPatchMatrix.map(row => row.id)).toEqual(['SCN-IFM-05', 'SCN-IFM-06'])
  const created = await client.payments.createOrder(
    ownedMerchantId,
    {
      amountMinor: 1900,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'P428'),
    },
    uniqueIdempotencyKey(testInfo, 'P428'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const missing = await client.payments.patch(
    ownedMerchantId,
    paymentOrderId,
    { metadata: { note: 'pom-rest' } },
  )
  expectStatus(missing, 428)
  expectProblem(missing.body, 428)

  const stale = await client.payments.patch(
    ownedMerchantId,
    paymentOrderId,
    { metadata: { note: 'pom-stale' } },
    '"v99"',
  )
  expectStatus(stale, 412)
  expectProblem(stale.body, 412)

  const before = await client.payments.get(ownedMerchantId, paymentOrderId)
  const etag = etagOf(before.headers)
  expect(etag).toBeTruthy()
  const patched = await client.payments.patch(
    ownedMerchantId,
    paymentOrderId,
    { metadata: { note: 'pom-ok' } },
    etag,
  )
  expectStatus(patched, 200)
  const after = await client.payments.get(ownedMerchantId, paymentOrderId)
  expect(etagOf(after.headers)).toBeTruthy()
  expect(etagOf(after.headers)).not.toBe(etag)
})
