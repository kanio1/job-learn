import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { waitForBffResponse } from '../utils/wait-bff'
import { expectStatus } from '../api/bff-client'
import { z } from 'zod'

const summarySchema = z.object({
  byStatus: z.array(z.object({ status: z.string(), orderCount: z.number() }).passthrough()),
}).passthrough()

test.describe('Payment summary chart', () => {
  test('PW-M360-API-052 GET summary 200 matches Zod-shaped byStatus', async ({ api, ownedMerchantId }, testInfo) => {
    const client = api
    const reference = uniqueOrderReference(testInfo, 'SUM')
    expect((await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 2100, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'SUM'),
    )).status).toBe(201)

    const summary = await client.payments.summary(ownedMerchantId)
    expectStatus(summary, 200)
    expect(summary.body.byStatus).toBeDefined()
  })

  test('PW-M360-E2E-120 legend counts equal summary JSON', async ({ app, api, page, ownedMerchantId }, testInfo) => {
    const client = api
    const reference = uniqueOrderReference(testInfo, 'CHART')
    expect((await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 2200, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'CHART'),
    )).status).toBe(201)

    const summaryPath = `/api/merchants/${ownedMerchantId}/payment-orders/summary`
    const summaryResponse = waitForBffResponse(page, { method: 'GET', pathExact: summaryPath })
    await app.payments.gotoForMerchant(ownedMerchantId)
    const response = await summaryResponse
    expect(response.status()).toBe(200)
    const body = summarySchema.parse(await response.json())
    await expect(app.payments.statusChart()).toBeVisible()
    for (const row of body.byStatus) {
      await expect(app.payments.statusLegend(row.status)).toHaveText(`${row.status} ${row.orderCount}`)
    }
  })

  test('PW-M360-E2E-121 403 summary without payments read', async ({ actors, ownedMerchantId }) => {
    const { page, app: deniedApp } = await actors.open('merchantDenied')
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
      await expect(deniedApp.payments.paymentAccessDenied()).toBeVisible()
  })
})
