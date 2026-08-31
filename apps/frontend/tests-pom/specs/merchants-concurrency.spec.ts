import { uniqueMerchantReference } from '../data/factories'
import { expectStatus } from '../api/bff-client'
import { test, expect } from '../fixtures'
import { etagOf, expectProblem } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { merchantIfMatchMatrix } from '../methods/decision-table/MerchantIfMatchMatrix'

test.describe('Merchant ETag / If-Match', { tag: ['@security'] }, () => {
  test('PW-M360-API-041 BFF activate stale ETag is 412', async ({ api }, testInfo) => {
    expect(merchantIfMatchMatrix.find(row => row.testId === 'RA-M360-052')?.expectStatus).toBe(412)
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Stale ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!
    const stale = await client.merchants.activate(merchantId, '"v99"')
    expectStatus(stale, 412)
    expectProblem(stale.body, 412, 'merchant_version_mismatch')
  })

  test('PW-M360-SEC-020 two contexts stale If-Match shows Reload', async ({
    api,
    actors,
  }, testInfo) => {
    const seed = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.merchants.create(reference, `Race ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const firstAdmin = await actors.open('platformAdmin')
    const secondAdmin = await actors.open('platformAdmin')
    await firstAdmin.app.merchantDetail.gotoMerchant(merchantId)
    await firstAdmin.app.merchantDetail.expectLoaded()
    await expect(firstAdmin.app.merchantDetail.statusBadge()).toContainText('Draft')

    const current = await secondAdmin.api.merchants.get(merchantId)
    expect((await secondAdmin.api.merchants.activate(merchantId, etagOf(current.headers)!)).status).toBe(200)

    const activate = waitForBffResponse(firstAdmin.page, {
      method: 'POST',
      pathExact: `/api/merchants/${merchantId}/activate`,
    })
    await firstAdmin.app.merchantDetail.activate()
    expect((await activate).status()).toBe(412)
    await firstAdmin.app.problem.expectVisible()
    await expect(firstAdmin.app.problem.statusBadge(412)).toBeVisible()
    await expect(firstAdmin.app.merchantDetail.reloadButton()).toBeVisible()

    const reloaded = waitForBffResponse(firstAdmin.page, {
      method: 'GET',
      pathExact: `/api/merchants/${merchantId}`,
    })
    await firstAdmin.app.merchantDetail.reloadAfterConflict()
    expect((await reloaded).status()).toBe(200)
    await expect(firstAdmin.app.merchantDetail.statusBadge()).toContainText('Active')
    await expect(firstAdmin.app.problem.root()).toHaveCount(0)
  })
})
