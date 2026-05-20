import { expect, test } from '@playwright/test'
import { merchant, mockAuthenticatedSession, mockMerchantApi, uniqueReference } from './merchant-support'

test('activates and suspends a merchant', async ({ page }) => {
  const reference = uniqueReference('LIFE')
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [merchant(reference)])

  await page.goto('/admin/merchants')

  await page.getByRole('button', { name: `Activate ${reference}` }).click()
  await expect(page.getByText('ACTIVE')).toBeVisible()

  await page.getByRole('button', { name: `Suspend ${reference}` }).click()
  await expect(page.getByText('SUSPENDED', { exact: true }).first()).toBeVisible()
})
