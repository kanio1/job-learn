import { expect, test } from '@playwright/test'

test('renders the Phase 0 dashboard foundation shell', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Payment Quality Engineering Lab' })).toBeVisible()
  await expect(page.getByText('Running skeleton, not payment functionality')).toBeVisible()
  await expect(page.getByText('Merchant dashboard')).toBeVisible()
  await expect(page.getByText('Risk and review')).toBeVisible()
})
