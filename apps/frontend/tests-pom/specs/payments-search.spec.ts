import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'

test('PW-OPS-SEC-040 manager search has no Merchants group and shows own payment', async ({
  app,
  api,
  ownedMerchantId,
}, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'SRCH')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 2100, currency: 'EUR', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'SRCH'),
  )
  expect(created.status).toBe(201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.commandPalette.openWithKeyboard()
  const search = app.page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    try {
      const url = new URL(response.url())
      return url.pathname === '/api/search' && url.searchParams.get('q') === reference
    }
    catch {
      return false
    }
  })
  await app.commandPalette.search(reference)
  expect((await search).status()).toBe(200)
  await expect(app.commandPalette.dialog().getByRole('option', { name: reference })).toBeVisible()
  await expect(app.commandPalette.dialog().getByText('Merchants', { exact: true })).toHaveCount(0)
  await expect(app.commandPalette.dialog().getByText('Payments', { exact: true })).toBeVisible()
})
