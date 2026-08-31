import { z } from 'zod'
import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueMerchantReference, uniqueOrderReference, uniqueToken } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { expectError } from '../api/contracts/http-result'
import { expectMerchantError, expectProblem } from '../utils/http'

const merchantListPageSchema = z.object({
  content: z.array(z.object({ merchantId: z.string() }).passthrough()),
  page: z.number().int().nonnegative(),
  size: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
})
const merchantCreatedSchema = z.object({
  error: z.string().optional(),
  merchantId: z.string().optional(),
}).passthrough()

test('platform admin POST payment-order is 403', async ({ api }, testInfo) => {
  const client = api
  const created = await client.payments.createOrder(
    merchantAlphaId,
    {
      amountMinor: 900,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'ADM403'),
    },
    uniqueIdempotencyKey(testInfo, 'ADM403'),
  )
  expectStatus(created, 403)
  expectProblem(created.body, 403)
})

test('POST merchant without tenantReference is 400', async ({ api }) => {
  const client = api
  const created = await client.merchants.create(`NO-TENANT-${uniqueToken()}`, 'Missing tenant', null)
  expectStatus(created, 400)
  expectMerchantError(created.body, 'validation')
})

test('POST merchant without CSRF token is 201 not csrf_failed', { tag: ['@security'] }, async ({ page }, testInfo) => {
  const merchantReference = uniqueMerchantReference(testInfo)
  const response = await page.request.post('/api/merchants', {
    data: {
      merchantReference,
      displayName: `CSRF contrast ${merchantReference}`,
      tenantReference: 'TENANT_ALPHA',
    },
  })
  expect(response.status()).toBe(201)
  const body = merchantCreatedSchema.parse(await response.json())
  expect(body.error).not.toBe('csrf_failed')
  expect(body.merchantId).toBeTruthy()
})

test('GET unknown merchant is 404', async ({ api }) => {
  const client = api
  const missing = await client.merchants.get('00000000-0000-0000-0000-000000000000')
  const error = expectError(missing, 404)
  expectProblem(error.body, 404)
})

test('platform admin expiration sweep is 200 with expiredCount', async ({ api }) => {
  const client = api
  const sweep = await client.operations.runExpirationSweep()
  expectStatus(sweep, 200)
  expect(sweep.body.expiredCount).toEqual(expect.any(Number))
})

test('PW-M360-API-001 GET merchants returns content and totalElements', async ({ api }) => {
  const client = api
  const listed = await client.merchants.list({ page: 0, size: 20 })
  expectStatus(listed, 200)
  const body = merchantListPageSchema.parse(listed.body)
  expect(body.content.length).toBeLessThanOrEqual(20)
  expect(body.totalElements).toEqual(expect.any(Number))
})

test('PW-M360-API-002 illegal sort is 400 problem+json', async ({ api }) => {
  const client = api
  const listed = await client.merchants.list({ sort: 'revenue,desc' })
  expectStatus(listed, 400)
  expect(listed.headers['content-type'] ?? '').toContain('application/problem+json')
  expectMerchantError(listed.body, 'validation')
})

test('PW-M360-API-003 payment list status=CAPTURED is 200', async ({ api }) => {
  const client = api
  const listed = await client.payments.list(merchantAlphaId, { status: 'CAPTURED' })
  expectStatus(listed, 200)
})

test('PW-M360-API-004 merchant list Zod safeParse', async ({ api }) => {
  const client = api
  const listed = await client.merchants.list()
  expectStatus(listed, 200)
  expect(merchantListPageSchema.safeParse(listed.body).success).toBe(true)
})
