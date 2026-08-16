import { expect, test } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { mockAuthenticatedSession, mockMerchantApi } from './merchant-support'

test('login page has no serious axe violations', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByTestId('login-control')).toBeVisible()
  const results = await new AxeBuilder({ page })
    .exclude('nuxt-devtools-frame')
    .disableRules(['color-contrast'])
    .analyze()
  const serious = results.violations.filter(v => v.impact === 'serious' || v.impact === 'critical')
  expect(serious, JSON.stringify(serious, null, 2)).toEqual([])
})

test('merchants list has no serious axe violations', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [])
  await page.goto('/admin/merchants')
  await expect(page.getByRole('heading', { name: 'Merchants' })).toBeVisible()
  const results = await new AxeBuilder({ page })
    .exclude('nuxt-devtools-frame')
    .disableRules(['color-contrast'])
    .analyze()
  const serious = results.violations.filter(v => v.impact === 'serious' || v.impact === 'critical')
  expect(serious, JSON.stringify(serious, null, 2)).toEqual([])
})
