import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

test('renders loading state while merchant list is pending', async ({ page }) => {
  await mockAuthenticatedSession(page)
  let releaseResponse: () => void
  const responseHeld = new Promise<void>((resolve) => {
    releaseResponse = resolve
  })

  await page.route('**/api/merchants', async route => {
    await responseHeld
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ merchants: [] }) })
  })

  const merchantsRequest = page.waitForRequest(request =>
    request.method() === 'GET' && new URL(request.url()).pathname === '/api/merchants'
  )
  const navigation = page.goto('/admin/merchants')
  await merchantsRequest

  await expect(page.getByRole('status', { name: 'Loading merchants…' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Registry is empty' })).not.toBeVisible()

  releaseResponse!()
  await navigation

  await expect(page.getByRole('status', { name: 'Loading merchants…' })).not.toBeVisible()
  await expect(page.getByRole('heading', { name: 'Registry is empty' })).toBeVisible()
})

test('renders recoverable merchant list error state', async ({ page }) => {
  await mockAuthenticatedSession(page)
  let attempts = 0
  await page.route('**/api/merchants', async route => {
    attempts += 1
    if (attempts <= 2) {
      await route.fulfill({
        status: 503,
        contentType: 'application/problem+json',
        body: JSON.stringify({
          type: 'https://example.test/problems/merchant-service-unavailable',
          title: 'Merchant service unavailable',
          status: 503,
          detail: 'Merchant registry is temporarily unavailable.',
        }),
      })
      return
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ merchants: [] }) })
  })

  await page.goto('/admin/merchants')

  await expect(page.getByRole('alert', { name: 'Request failed' })).toContainText('Merchant service unavailable')
  await expect(page.getByText('Merchant registry is temporarily unavailable.')).toBeVisible()
  await page.getByRole('button', { name: 'Retry' }).click()
  await expect(page.getByRole('heading', { name: 'Registry is empty' })).toBeVisible()
  await expect(page.getByRole('alert', { name: 'Request failed' })).not.toBeVisible()
  expect(attempts).toBe(3)
})
