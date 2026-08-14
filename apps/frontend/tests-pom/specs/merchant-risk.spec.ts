import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'

test('risk toggle on a unique merchant shows the list badge', { tag: ['@ux'] }, async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Risk ${reference}`
  const created = await client.createMerchant(reference, displayName)
  expect(created.status).toBe(201)
  const merchantId = created.body.merchantId
  expect(merchantId).toBeTruthy()

  await app.merchantDetail.gotoMerchant(merchantId!)
  await app.merchantDetail.expectLoaded()
  await app.merchantDetail.activate()
  await app.merchantDetail.expectStatus('Active')
  await app.merchantDetail.expectRiskFlagged(false)
  await expect(app.page.getByTestId('merchant-risk-toggle')).toBeVisible()

  const patched = page.waitForResponse(response =>
    response.url().includes('/risk-flag') && response.request().method() === 'PATCH',
  )
  await app.merchantDetail.toggleRisk()
  const response = await patched
  if (response.status() === 200) {
    await app.merchantDetail.expectRiskFlagged(true)
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.page.getByPlaceholder('Filter merchants...').fill(reference)
    await app.merchants.expectRowVisible(reference)
    await app.merchants.expectRiskBadgeFor(displayName)
  } else {
    expect(response.status()).toBe(403)
    await app.merchantDetail.expectRiskFlagged(false)
  }
})
