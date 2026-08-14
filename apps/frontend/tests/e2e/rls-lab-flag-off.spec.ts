import { expect, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'

test('rls lab nav and page are absent when the public flag is off', async ({ page }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await page.goto('/admin/merchants')
  await expect(page.getByTestId('nav-link-rls-lab')).toHaveCount(0)

  await page.goto('/admin/rls-lab')
  await expect(page.getByTestId('rls-lab-items-table')).toHaveCount(0)
  await expect(page.getByText(/404|not found/i).first()).toBeVisible()
})

test('rls lab BFF returns 404 when the public flag is off', async ({ page, request }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  const response = await request.get('/api/rls-lab/items')
  expect(response.status()).toBe(404)
})
