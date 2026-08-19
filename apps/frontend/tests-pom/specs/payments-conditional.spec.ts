import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { etagOf, expectNoAuthTokenLeak, expectProblem, headerOf } from '../utils/http'

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
  expect(headerOf(initial.headers, 'vary') ?? '').toMatch(/authorization/i)

  const conditional = await client.getPaymentOrder(ownedMerchantId, paymentOrderId!, {
    'If-None-Match': etag!,
  })
  expect(conditional.status).toBe(304)
  expect(conditional.raw ?? '').toBe('')
  expect(etagOf(conditional.headers)).toBe(etag)
  expect(headerOf(conditional.headers, 'cache-control') ?? '').toMatch(/no-store/i)
  expect(headerOf(conditional.headers, 'vary') ?? '').toMatch(/authorization/i)

  const head = await client.headPaymentOrder(ownedMerchantId, paymentOrderId!)
  expect(head.status).toBe(200)
  expect(head.raw).toBe('')
  expect(etagOf(head.headers)).toBeTruthy()
  expectNoAuthTokenLeak(head.headers, head.raw)
})

test('PATCH metadata without If-Match is 428 problem+json', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
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
  const patched = await client.patchPaymentOrder(
    ownedMerchantId,
    created.body.paymentOrderId!,
    { metadata: { note: 'pom-rest' } },
  )
  expect(patched.status).toBe(428)
  expectProblem(patched.body, 428)
})
