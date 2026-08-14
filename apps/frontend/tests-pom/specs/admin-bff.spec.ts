import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'

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
})

test('POST merchant without tenantReference is 400', async ({ api }) => {
  const client = requireApi(api)
  const created = await client.createMerchant(`NO-TENANT-${uniqueToken()}`, 'Missing tenant', null)
  expect(created.status).toBe(400)
})

test('GET unknown merchant is 404', async ({ api }) => {
  const client = requireApi(api)
  const missing = await client.getMerchant('00000000-0000-0000-0000-000000000000')
  expect(missing.status).toBe(404)
})
