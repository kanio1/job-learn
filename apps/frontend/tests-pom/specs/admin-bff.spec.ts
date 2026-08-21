import { z } from 'zod'
import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueMerchantReference, uniqueOrderReference, uniqueToken } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectMerchantError, expectProblem } from '../utils/http'

const merchantListPageSchema = z.object({
  content: z.array(z.object({ merchantId: z.string() }).passthrough()),
  page: z.number().int().nonnegative(),
  size: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
})

test('platform admin POST payment-order is 403', async ({ api }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    {
      amountMinor: 900,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'ADM403'),
    },
    uniqueIdempotencyKey(testInfo, 'ADM403'),
  )
  expect(created.status).toBe(403)
  expectProblem(created.body, 403)
})

test('POST merchant without tenantReference is 400', async ({ api }) => {
  const client = requireApi(api)
  const created = await client.createMerchant(`NO-TENANT-${uniqueToken()}`, 'Missing tenant', null)
  expect(created.status).toBe(400)
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
  const body = await response.json() as { error?: string, merchantId?: string }
  expect(body.error).not.toBe('csrf_failed')
  expect(body.merchantId).toBeTruthy()
})

test('GET unknown merchant is 404', async ({ api }) => {
  const client = requireApi(api)
  const missing = await client.getMerchant('00000000-0000-0000-0000-000000000000')
  expect(missing.status).toBe(404)
  expectProblem(missing.body, 404)
})

test('platform admin expiration sweep is 200 with expiredCount', async ({ api }) => {
  const client = requireApi(api)
  const sweep = await client.runExpirationSweep()
  expect(sweep.status).toBe(200)
  expect(typeof sweep.body?.expiredCount).toBe('number')
})

test('PW-M360-API-001 GET merchants returns content and totalElements', async ({ api }) => {
  const client = requireApi(api)
  const listed = await client.listMerchants({ page: 0, size: 20 })
  expect(listed.status).toBe(200)
  expect(Array.isArray(listed.body?.content)).toBe(true)
  expect(listed.body!.content!.length).toBeLessThanOrEqual(20)
  expect(typeof listed.body?.totalElements).toBe('number')
})

test('PW-M360-API-002 illegal sort is 400 problem+json', async ({ api }) => {
  const client = requireApi(api)
  const listed = await client.listMerchants({ sort: 'revenue,desc' })
  expect(listed.status).toBe(400)
  expect(listed.headers['content-type'] ?? '').toContain('application/problem+json')
  expectMerchantError(listed.body, 'validation')
})

test('PW-M360-API-003 payment list status=CAPTURED is 200', async ({ api }) => {
  const client = requireApi(api)
  const listed = await client.listPaymentOrders(merchantAlphaId, { status: 'CAPTURED' })
  expect(listed.status).toBe(200)
})

test('PW-M360-API-004 merchant list Zod safeParse', async ({ api }) => {
  const client = requireApi(api)
  const listed = await client.listMerchants()
  expect(listed.status).toBe(200)
  expect(merchantListPageSchema.safeParse(listed.body).success).toBe(true)
})
