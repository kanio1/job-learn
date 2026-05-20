import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession, mockMerchantApi } from './merchant-support'

test('renders the Phase 1 merchant registry shell', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [])

  await page.goto('/admin/merchants')

  await expect(page.getByRole('heading', { name: 'Merchants' })).toBeVisible()

  await page.getByRole('button', { name: 'Search...' }).click()
  await expect(page.getByText('Search Payment Quality Lab')).toBeVisible()
  await expect(page.getByText('Quickly navigate to dashboard areas and merchant registry actions.')).toBeVisible()
  await expect(page.getByText('dashboardSearch.title')).toHaveCount(0)
  await expect(page.getByText('dashboardSearch.description')).toHaveCount(0)
})
