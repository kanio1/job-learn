import { expect, test } from '@playwright/test'

test('displays payment order detail', async ({ page }) => {
  const merchantId = '11111111-1111-1111-1111-111111111111'
  const paymentOrderId = '33333333-3333-3333-3333-333333333333'

  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        paymentOrderId,
        merchantId,
        clientOrderReference: 'PAY-READ-001',
        amountMinor: 5000,
        currency: 'EUR',
        status: 'CREATED',
        createdAt: '2026-05-27T10:00:00Z',
        updatedAt: '2026-05-27T10:00:00Z',
      }),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)

  await expect(page.getByText('Payment Order Detail')).toBeVisible({ timeout: 10000 })
  await expect(page.getByText(paymentOrderId)).toBeVisible()
  await expect(page.getByText('CREATED')).toBeVisible()
  await expect(page.getByText('5000 minor units')).toBeVisible()
  await expect(page.getByText('EUR')).toBeVisible()
  await expect(page.getByText('PAY-READ-001')).toBeVisible()
})
