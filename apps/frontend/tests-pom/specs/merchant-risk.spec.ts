import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'

test('risk toggle on a unique merchant shows the list badge', { tag: ['@ux'] }, async ({ app, api }, testInfo) => {
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

  await app.merchantDetail.toggleRisk()
  await app.merchantDetail.expectRiskFlagged(true)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.page.getByLabel('Filter merchants').fill(reference)
  await app.merchants.expectRowVisible(reference)
  await app.merchants.expectRiskBadgeFor(displayName)
})
