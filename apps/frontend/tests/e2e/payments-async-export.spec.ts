import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

const merchantId = '11111111-1111-4111-8111-111111111111'
const jobId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'

test('async export polls until READY then downloads CSV', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ totalOrders: 0, totalAmountMinor: 0, byCurrency: [], byStatus: [] }),
    })
  })
  await page.route(`**/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}/content`, async (route) => {
    await route.fulfill({
      status: 200,
      headers: { 'Content-Disposition': 'attachment; filename="export.csv"', 'Content-Type': 'text/csv' },
      body: 'paymentOrderId\n',
    })
  })
  await page.route(`**/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ jobId, status: 'READY' }),
    })
  })
  await page.route(`**/api/merchants/${merchantId}/payment-orders/export-jobs`, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 202,
        headers: {
          Location: `/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ jobId, status: 'PENDING' }),
      })
      return
    }
    await route.fallback()
  })
  await page.route(`**/api/merchants/${merchantId}/payment-orders**`, async (route) => {
    if (route.request().url().includes('/summary') || route.request().url().includes('/export')) {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await page.getByTestId('export-payment-orders-async').click()
  await expect(page.getByTestId('async-export-status')).toContainText('READY')
})
