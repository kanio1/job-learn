import { uniqueMerchantReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'

test('risk toggle on a unique merchant shows the list badge', { tag: ['@ux'] }, async ({ app, api, page }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Risk ${reference}`
  const created = await client.merchants.create(reference, displayName)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId
  expect(merchantId).toBeTruthy()

  await app.merchantDetail.gotoMerchant(merchantId!)
  await app.merchantDetail.expectLoaded()
  await app.merchantDetail.activate()
  await expect(app.merchantDetail.statusBadge()).toContainText('Active')
  await expect(app.merchantDetail.riskStatus()).toContainText('No risk flag')
  await expect(app.merchantDetail.riskToggle()).toBeVisible()

  const patched = page.waitForResponse(response =>
    response.url().includes('/risk-flag') && response.request().method() === 'PATCH',
  )
  await app.merchantDetail.toggleRisk()
  const response = await patched
  if (response.status() === 200) {
    await expect(app.merchantDetail.riskStatus()).toContainText('Risk flagged')
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
    await expect(app.merchants.riskBadgeFor(displayName)).toBeVisible()
  } else {
    expect(response.status()).toBe(403)
    await expect(app.merchantDetail.riskStatus()).toContainText('No risk flag')
  }
})
