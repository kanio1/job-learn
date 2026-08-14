import { expect, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'

test('rls lab hub is reachable when flag is on', async ({ page }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await page.route('**/api/rls-lab/items', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [{
          itemId: '00000000-0000-0000-0000-0000000000a1',
          tenantId: '00000000-0000-0000-0000-0000000000aa',
          label: 'Alpha secret',
          amountMinor: 100,
        }],
      }),
    })
  })
  await page.goto('/admin/merchants')
  await expect(page.getByTestId('nav-link-rls-lab')).toBeVisible({ timeout: 15_000 })
  await page.getByTestId('nav-link-rls-lab').click()
  await expect(page.getByText('Java WHERE is not RLS')).toBeVisible()
  await expect(page.getByTestId('rls-lab-items-table')).toBeVisible()
  await expect(page.getByTestId('rls-lab-compare-panel')).toBeVisible()
})

test('merchant role hides compare panel in mocked session', async ({ page }) => {
  await mockRoleSession(page, 'MERCHANT_MANAGER')
  await page.route('**/api/rls-lab/items', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [{
          itemId: '00000000-0000-0000-0000-0000000000a1',
          tenantId: '00000000-0000-0000-0000-0000000000aa',
          label: 'Alpha secret',
          amountMinor: 100,
        }],
      }),
    })
  })
  await page.goto('/admin/rls-lab')
  await expect(page.getByTestId('rls-lab-items-table')).toBeVisible({ timeout: 15_000 })
  await expect(page.getByTestId('rls-lab-compare-panel')).toHaveCount(0)
})
