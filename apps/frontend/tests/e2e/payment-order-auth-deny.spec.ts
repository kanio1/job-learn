import { expect, test } from '@playwright/test'

test('unauthenticated user is redirected from payment create page', async ({ page }) => {
  const merchantId = '11111111-1111-1111-1111-111111111111'

  await page.goto(`/admin/merchants/${merchantId}/payments/new`)

  await expect(page).toHaveURL(/.*login.*/, { timeout: 10000 })
})

test('unauthenticated user is redirected from payment detail page', async ({ page }) => {
  const merchantId = '11111111-1111-1111-1111-111111111111'
  const paymentOrderId = '44444444-4444-4444-4444-444444444444'

  await page.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)

  await expect(page).toHaveURL(/.*login.*/, { timeout: 10000 })
})
