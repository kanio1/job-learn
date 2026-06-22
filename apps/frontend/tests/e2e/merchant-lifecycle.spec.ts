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

// ---------------------------------------------------------------------------
// Validates: Requirements 2.6, 12.2 — activate action via data-testid locator;
// resulting status is shown from the success response
// ---------------------------------------------------------------------------

test('activates a PENDING merchant via data-testid button and shows ACTIVE status', async ({ page }) => {
  const reference = uniqueReference('ACT')
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [merchant(reference, 'PENDING')])

  await page.goto('/admin/merchants')

  // Use the stable test-id locator (Req 12.2)
  const activateButton = page.getByTestId('activate-merchant-button')
  await expect(activateButton).toBeVisible()
  await activateButton.click()

  // Status from the success response is displayed (Req 2.6)
  await expect(page.getByText('ACTIVE')).toBeVisible()
})

test('activates a SUSPENDED merchant via data-testid button and shows ACTIVE status', async ({ page }) => {
  const reference = uniqueReference('REACTIVATE')
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [merchant(reference, 'SUSPENDED')])

  await page.goto('/admin/merchants')

  const activateButton = page.getByTestId('activate-merchant-button')
  await expect(activateButton).toBeVisible()
  await activateButton.click()

  await expect(page.getByText('ACTIVE')).toBeVisible()
})

// ---------------------------------------------------------------------------
// Validates: Requirements 2.3, 9.3 — empty state rendered when merchant list
// is empty; data-testid="empty-state" is present (Req 12.1 testability rule)
// ---------------------------------------------------------------------------

test('shows empty-state when merchant list returns no items', async ({ page }) => {
  await mockAuthenticatedSession(page)

  // Override: return an empty list deterministically (Req 2.3)
  await page.route('**/api/merchants', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ merchants: [] })
    })
  })
  // Stub session
  await page.route('**/api/_auth/session', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ loggedIn: true, user: { username: 'platform.operator' } })
    })
  })

  await page.goto('/admin/merchants')

  // EmptyStateCard renders data-testid="empty-state" (Req 2.3, 9.3)
  await expect(page.getByTestId('empty-state')).toBeVisible()
  // Meaningful description text
  await expect(page.getByText('No merchants have been registered yet')).toBeVisible()
})

// ---------------------------------------------------------------------------
// Validates: Requirements 2.2, 9.4 — error state rendered when GET /api/merchants
// returns a failure; data-testid="error-state" is present
// ---------------------------------------------------------------------------

test('shows error-state when GET /api/merchants returns a server error', async ({ page }) => {
  await mockAuthenticatedSession(page)

  await page.route('**/api/merchants', async route => {
    await route.fulfill({
      status: 500,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        type: 'https://example.com/problems/internal-error',
        title: 'Internal Server Error',
        status: 500,
        detail: 'An unexpected error occurred processing the merchant list.',
        instance: '/api/merchants'
      })
    })
  })

  await page.goto('/admin/merchants')

  // ErrorState renders data-testid="error-state" (Req 2.2, 9.4)
  await expect(page.getByTestId('error-state')).toBeVisible()
})

test('shows error-state when GET /api/merchants returns 503 unreachable', async ({ page }) => {
  await mockAuthenticatedSession(page)

  await page.route('**/api/merchants', async route => {
    await route.fulfill({
      status: 503,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Backend unavailable' })
    })
  })

  await page.goto('/admin/merchants')

  await expect(page.getByTestId('error-state')).toBeVisible()
})
