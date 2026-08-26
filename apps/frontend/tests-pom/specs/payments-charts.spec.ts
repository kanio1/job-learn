import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { waitForBffResponse } from '../utils/wait-bff'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'
import { expectStatus } from '../api/bff-client'

test.describe('Payment summary chart', () => {
  test('PW-M360-API-052 GET summary 200 matches Zod-shaped byStatus', async ({ api, ownedMerchantId }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'SUM')
    expect((await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 2100, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'SUM'),
    )).status).toBe(201)

    const summary = await client.getPaymentOrdersSummary(ownedMerchantId)
    expectStatus(summary, 200)
    expect(Array.isArray(summary.body.byStatus)).toBe(true)
    expect(summary.body.byStatus?.every(row =>
      typeof row.status === 'string' && typeof row.orderCount === 'number',
    )).toBe(true)
  })

  test('PW-M360-E2E-120 legend counts equal summary JSON', async ({ app, api, page, ownedMerchantId }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'CHART')
    expect((await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 2200, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'CHART'),
    )).status).toBe(201)

    const summaryPath = `/api/merchants/${ownedMerchantId}/payment-orders/summary`
    const summaryResponse = waitForBffResponse(page, { method: 'GET', pathExact: summaryPath })
    await app.payments.gotoForMerchant(ownedMerchantId)
    const response = await summaryResponse
    expect(response.status()).toBe(200)
    const body = await response.json() as {
      byStatus: Array<{ status: string, orderCount: number }>
    }
    await expect(app.payments.statusChart()).toBeVisible()
    for (const row of body.byStatus) {
      await expect(app.payments.statusLegend(row.status)).toHaveText(`${row.status} ${row.orderCount}`)
    }
  })

  test('PW-M360-E2E-121 403 summary without payments read', async ({ browser, ownedMerchantId }) => {
    const context = await browser.newContext({ storageState: pomAuthFiles.merchantDenied })
    const page = await context.newPage()
    const deniedApp = new App(page)
    try {
      const denied = page.waitForResponse((response) => {
        try {
          return response.request().method() === 'GET'
            && new URL(response.url()).pathname === `/api/merchants/${ownedMerchantId}/payment-orders/summary`
        }
        catch {
          return false
        }
      })
      await deniedApp.payments.gotoForMerchant(ownedMerchantId)
      expect((await denied).status()).toBe(403)
      await expect(page.getByRole('alert').filter({
        hasText: 'You do not have permission to view payment orders',
      })).toBeVisible()
    }
    finally {
      await context.close()
    }
  })
})
