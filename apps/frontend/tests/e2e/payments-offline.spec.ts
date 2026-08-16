import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

const merchantId = '11111111-1111-4111-8111-111111111111'

test('shows offline banner when the browser loses network', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalOrders: 0,
        totalAmountMinor: 0,
        byCurrency: [],
        byStatus: [],
      }),
    })
  })
  await page.route(`**/api/merchants/${merchantId}/payment-orders**`, async (route) => {
    if (route.request().url().includes('/summary')) {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    })
  })
  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await expect(page.getByRole('heading', { name: 'Payment Orders', exact: true })).toBeVisible()
  await context.setOffline(true)
  await page.evaluate(() => window.dispatchEvent(new Event('offline')))
  await expect(page.getByTestId('payments-offline-banner')).toBeVisible()
  await context.setOffline(false)
})
