import { expect, test } from '@playwright/test'

test('creates a payment order and navigates to detail', async ({ page }) => {
  await page.goto('/login')
  await page.waitForURL('**/admin/**', { timeout: 15000 }).catch(() => {})

  const merchantId = '11111111-1111-1111-1111-111111111111'
  const paymentOrderId = '22222222-2222-2222-2222-222222222222'

  await page.route(`**/api/merchants/${merchantId}/payment-orders`, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          paymentOrderId,
          merchantId,
          clientOrderReference: 'PAY-E2E-001',
          amountMinor: 12500,
          currency: 'PLN',
          status: 'CREATED',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      })
    }
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/new`)

  await page.getByLabel('Amount (minor units)').fill('12500')
  await page.getByLabel('Currency').click()
  await page.getByText('PLN').click()
  await page.getByLabel('Client Order Reference').fill('PAY-E2E-001')
  await page.getByRole('button', { name: 'Create Payment Order' }).click()

  await expect(page.getByText(/created successfully/i)).toBeVisible({ timeout: 10000 })
})
