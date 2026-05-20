import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

test('unauthenticated access starts Keycloak redirect and hides merchant data', async ({ page }) => {
  await page.route('**/auth/keycloak', async route => {
    await route.fulfill({ status: 200, contentType: 'text/html', body: '<h1>Keycloak login</h1>' })
  })

  await page.goto('/admin/merchants')

  await expect(page.getByRole('heading', { name: 'Keycloak login' })).toBeVisible()
  await expect(page.getByText('Merchant registry')).toHaveCount(0)
})

test('authenticated identity without merchant authority sees deterministic denial', async ({ page }) => {
  await mockAuthenticatedSession(page, 'merchant.denied')
  await page.route('**/api/merchants', async route => {
    await route.fulfill({
      status: 403,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'forbidden', message: 'Forbidden' })
    })
  })

  await page.goto('/admin/merchants')

  await expect(page.getByText('You do not have permission to view merchants')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Create merchant' })).toHaveCount(0)
})
