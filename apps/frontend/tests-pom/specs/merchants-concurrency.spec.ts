import { uniqueMerchantReference } from '../data/factories'
import { BffClient } from '../api/bff-client'
import { pomAuthFiles } from '../utils/env'
import { test, expect, requireApi } from '../fixtures'
import { App } from '../pages/App'
import { etagOf, expectProblem } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { merchantIfMatchMatrix } from '../methods/decision-table/MerchantIfMatchMatrix'

test.describe('Merchant ETag / If-Match', { tag: ['@security'] }, () => {
  test('PW-M360-API-041 BFF activate stale ETag is 412', async ({ api }, testInfo) => {
    expect(merchantIfMatchMatrix.find(row => row.testId === 'RA-M360-052')?.expectStatus).toBe(412)
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Stale ${reference}`)
    expect(created.status).toBe(201)
    const merchantId = created.body.merchantId!
    const stale = await client.activateMerchant(merchantId, '"v99"')
    expect(stale.status).toBe(412)
    expectProblem(stale.body, 412, 'merchant_version_mismatch')
  })

  test('PW-M360-SEC-020 two contexts stale If-Match shows Reload', async ({
    browser,
    playwright,
    api,
  }, testInfo) => {
    const seed = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.createMerchant(reference, `Race ${reference}`)
    expect(created.status).toBe(201)
    const merchantId = created.body.merchantId!

    const contextA = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
    const apiB = await BffClient.create(playwright, pomAuthFiles.platformAdmin)
    const appA = new App(await contextA.newPage())
    try {
      await appA.merchantDetail.gotoMerchant(merchantId)
      await appA.merchantDetail.expectLoaded()
      await appA.merchantDetail.expectStatus('Draft')

      const current = await apiB.getMerchant(merchantId)
      expect((await apiB.activateMerchant(merchantId, etagOf(current.headers)!)).status).toBe(200)

      const activate = waitForBffResponse(appA.page, {
        method: 'POST',
        pathExact: `/api/merchants/${merchantId}/activate`,
      })
      await appA.merchantDetail.activate()
      expect((await activate).status()).toBe(412)
      await appA.problem.expectVisible()
      await appA.problem.expectStatusBadge(412)
      await expect(appA.page.getByRole('button', { name: 'Reload' })).toBeVisible()

      const reloaded = waitForBffResponse(appA.page, {
        method: 'GET',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await appA.merchantDetail.reloadAfterConflict()
      expect((await reloaded).status()).toBe(200)
      await appA.merchantDetail.expectStatus('Active')
      await expect(appA.problem.root()).toHaveCount(0)
    }
    finally {
      await contextA.close()
      await apiB.dispose()
    }
  })
})
