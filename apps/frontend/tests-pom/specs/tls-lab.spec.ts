import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'

test('platform admin reaches RLS lab hub over the TLS origin', async ({ app }) => {
  await app.rlsLab.goto()
  await app.rlsLab.expectLoaded()
  expect(new URL(app.page.url()).protocol).toBe('https:')
  await expect(app.page).toHaveURL(/\/admin\/rls-lab/)
})

test('merchant manager applies a payment amount filter over the TLS origin', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'TLS')
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 7700, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'TLS'),
  )
  expect(created.status).toBe(201)

  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  await app.payments.applyAmountFilter(5000, 20000)
  await expect(app.page).toHaveURL(/minAmount=5000/)
  await app.payments.expectReferenceVisible(reference)
})
