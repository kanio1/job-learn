import { expect, test } from '@playwright/test'
import { merchant, mockAuthenticatedSession, mockMerchantApi, uniqueReference } from './merchant-support'

test('creates a merchant from the empty registry', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [])

  await page.goto('/admin/merchants')

  await expect(page.getByText('No merchants have been registered yet')).toBeVisible()
  await page.getByRole('button', { name: 'Create merchant' }).click()

  const reference = uniqueReference('CREATE')
  await page.getByLabel('Merchant reference').fill(reference)
  await page.getByLabel('Display name').fill('Created Merchant')
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  await expect(page.getByText('Merchant created', { exact: true })).toBeVisible()
  await expect(page.getByText(reference)).toBeVisible()
  await expect(page.getByText('DRAFT', { exact: true })).toBeVisible()
})

test('shows create validation and duplicate feedback', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [])

  await page.goto('/admin/merchants')
  await page.getByRole('button', { name: 'Create merchant' }).click()
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  await expect(page.getByText('Reference must be at least 3 characters')).toBeVisible()

  const reference = uniqueReference('DUP')
  await page.getByLabel('Merchant reference').fill(reference)
  await page.getByLabel('Display name').fill('Duplicate Merchant')
  await page.getByRole('button', { name: 'Create', exact: true }).click()
  await expect(page.getByText('Merchant created', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Create merchant' }).click()
  await page.getByLabel('Merchant reference').fill(reference)
  await page.getByLabel('Display name').fill('Duplicate Merchant')
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  await expect(page.getByText('A merchant with this reference already exists')).toBeVisible()
})

// ---------------------------------------------------------------------------
// Validates: Requirements 2.5, 10.1 — Validation gating: no request sent on
// invalid input; field-level messages are shown for each failing field
// ---------------------------------------------------------------------------

test('validation gating — empty form blocks submission and shows field messages', async ({ page }) => {
  await mockAuthenticatedSession(page)

  // Track whether POST /api/merchants is called at all
  let postCalled = false
  await page.route('**/api/merchants', async route => {
    if (route.request().method() === 'POST') {
      postCalled = true
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ merchants: [] })
    })
  })

  await page.goto('/admin/merchants')
  await page.getByRole('button', { name: 'Create merchant' }).click()

  // Both fields empty — click Create without filling anything
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  // Field-level validation messages must appear (Req 2.5, 10.2)
  await expect(page.getByText('Reference must be at least 3 characters')).toBeVisible()

  // No API request must have been sent (Req 10.1)
  expect(postCalled, 'POST /api/merchants must NOT be called when form is invalid').toBe(false)
})

test('validation gating — too-short reference is rejected with field message', async ({ page }) => {
  await mockAuthenticatedSession(page)

  let postCalled = false
  await page.route('**/api/merchants', async route => {
    if (route.request().method() === 'POST') {
      postCalled = true
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ merchants: [] })
    })
  })

  await page.goto('/admin/merchants')
  await page.getByRole('button', { name: 'Create merchant' }).click()

  // Fill a 2-char reference (below the 3-char minimum in the schema)
  await page.getByLabel('Merchant reference').fill('AB')
  await page.getByLabel('Display name').fill('Valid Name')
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  // Field message for the invalid field
  await expect(page.getByText('Reference must be at least 3 characters')).toBeVisible()

  // Still no API request
  expect(postCalled, 'POST /api/merchants must NOT be called when reference is too short').toBe(false)
})

// ---------------------------------------------------------------------------
// Validates: Requirements 12.1 — create-merchant-form data-testid is present
// and unique when the form is rendered
// ---------------------------------------------------------------------------

test('create-merchant-form data-testid is present and unique when form is open', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [])

  await page.goto('/admin/merchants')
  await page.getByRole('button', { name: 'Create merchant' }).click()

  // The form with the required test-id must be present (Req 12.1)
  const form = page.getByTestId('create-merchant-form')
  await expect(form).toBeVisible()

  // Uniqueness — exactly one element must match (Req 12.10)
  await expect(form).toHaveCount(1)
})

// ---------------------------------------------------------------------------
// Validates: Requirements 12.2 — activate-merchant-button data-testid is
// present and unique when a PENDING/SUSPENDED merchant is rendered in the table
// ---------------------------------------------------------------------------

test('activate-merchant-button data-testid is present for a pending merchant', async ({ page }) => {
  await mockAuthenticatedSession(page)
  const ref = uniqueReference('PENDING')
  await mockMerchantApi(page, [merchant(ref, 'PENDING')])

  await page.goto('/admin/merchants')

  // The activate button must carry the required test-id (Req 12.2)
  const activateButton = page.getByTestId('activate-merchant-button')
  await expect(activateButton).toBeVisible()

  // Exactly one button for a single merchant (Req 12.10)
  await expect(activateButton).toHaveCount(1)
})
