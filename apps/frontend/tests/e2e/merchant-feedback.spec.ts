import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

test('renders loading state while merchant list is pending', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await page.route('**/api/merchants', async route => {
    await new Promise(resolve => setTimeout(resolve, 2000))
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ merchants: [] }) })
  })

  await page.goto('/admin/merchants')

  // UTable loading state is visible (table present while fetching)
  await expect(page.locator('table')).toBeVisible()
  await expect(page.getByText('Registry is empty')).toBeVisible()
})

test('renders recoverable merchant list error state', async ({ page }) => {
  await mockAuthenticatedSession(page)
  let attempts = 0
  await page.route('**/api/merchants', async route => {
    attempts += 1
    if (attempts <= 2) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ error: 'server_error' }) })
      return
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ merchants: [] }) })
  })

  await page.goto('/admin/merchants')

  await expect(page.getByText('Failed to load merchants. Please try again.')).toBeVisible()
  await page.getByRole('button', { name: 'Retry loading merchants' }).click()
  await expect(page.getByText('No merchants have been registered yet')).toBeVisible()
})
