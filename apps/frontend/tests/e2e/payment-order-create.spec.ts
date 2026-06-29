import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

// ─── Constants ──────────────────────────────────────────────────────────────
// Valid RFC 4122 UUIDs: version nibble [1-8], variant nibble [89abAB]
// Zod 4's z.string().uuid() enforces this strictly.
const merchantId = '11111111-1111-4111-8111-111111111111'
const paymentOrderId = '22222222-2222-4222-8222-222222222222'
const createUrl = `**/api/merchants/${merchantId}/payment-orders`

/** Dismiss the Nuxt devtools error overlay if it appears (caused by pre-existing TS errors) */
async function dismissDevtoolsOverlay(page: import('@playwright/test').Page) {
  // Try to close the large overlay first (shown on first page load)
  const closeBtn = page.getByRole('button', { name: 'Close' })
  if (await closeBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
    await closeBtn.click()
    // Wait for the overlay to collapse before interacting with the page
    await closeBtn.waitFor({ state: 'hidden', timeout: 3000 }).catch(() => {})
  }
}

/**
 * Navigate to the payment create page and ensure the form is fully rendered.
 * Handles the Nuxt devtools overlay that appears on first page load in dev mode.
 *
 * Registers the session mock here rather than in each test because MERCHANT_MANAGER
 * role is required for canCreatePaymentOrder — without it the submit button is
 * disabled and getByRole('button', { name: 'Create Payment Order' }) fails.
 * Playwright LIFO route ordering means this handler wins over any earlier mock.
 */
async function gotoCreatePage(page: import('@playwright/test').Page, merchantId: string) {
  await page.route('**/api/_auth/session', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ loggedIn: true, user: { username: 'platform.operator', roles: ['MERCHANT_MANAGER'] } }),
    })
  })
  await page.goto(`/admin/merchants/${merchantId}/payments/new`)
  // Wait for the page heading — confirms the page has loaded past any redirect
  await expect(page.getByRole('heading', { name: 'New Payment Order' })).toBeVisible({ timeout: 15000 })
  await dismissDevtoolsOverlay(page)
  // Wait for the form submit button to be present (form rendered)
  await expect(page.getByRole('button', { name: 'Create Payment Order' })).toBeVisible({ timeout: 15000 })
}

// ─── Happy path (original test, preserved) ──────────────────────────────────

test('creates a payment order and navigates to detail', async ({ page }) => {
  // MERCHANT_MANAGER role required: canCreatePaymentOrder enables the submit button
  await page.route('**/api/_auth/session', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ loggedIn: true, user: { username: 'platform.operator', roles: ['MERCHANT_MANAGER'] } }),
    })
  })

  await page.route(`**/api/merchants/${merchantId}/payment-orders`, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          paymentOrderId,
          merchantId,
          clientOrderReference: 'PAY-E2E-001',
          amountMinor: 12500,
          currency: 'PLN',
          status: 'CREATED',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      })
    }
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/new`)
  await expect(page.getByRole('heading', { name: 'New Payment Order' })).toBeVisible({ timeout: 15000 })
  await dismissDevtoolsOverlay(page)

  await page.getByLabel('Amount (minor units)').fill('12500')
  await page.getByLabel('Currency').click()
  await page.locator('[data-slot="itemLabel"]').filter({ hasText: 'PLN' }).click()
  await page.getByLabel('Client Order Reference').fill('PAY-E2E-001')
  await page.getByRole('button', { name: 'Create Payment Order' }).click()

  // Use [data-slot="title"] to avoid matching the hidden ARIA live region that
  // also contains the toast text for accessibility announcements.
  await expect(page.locator('[data-slot="title"]').filter({ hasText: /created successfully/i })).toBeVisible({ timeout: 10000 })
})

// ─── Req 3.3 — Validation gating with real enum/bounds ──────────────────────

/**
 * amountMinor = 0 is below the schema minimum of 1.
 * The form must show a field-level error and NOT POST.
 * Validates: Requirement 3.3
 */
test('validation: amount 0 shows error and blocks submit', async ({ page }) => {
  await mockAuthenticatedSession(page)

  // If a POST somehow fires we want the test to fail explicitly
  let postFired = false
  await page.route(createUrl, async (route) => {
    if (route.request().method() === 'POST') {
      postFired = true
    }
    await route.fallback()
  })

  await gotoCreatePage(page, merchantId)

  // The form testid may be on UForm (which may not forward it to <form> DOM element)
  // so we use the submit button as the stable presence check and proceed to fill fields

  await page.getByLabel('Amount (minor units)').fill('0')
  await page.getByLabel('Currency').click()
  await page.locator('[data-slot="itemLabel"]').filter({ hasText: 'PLN' }).click()
  await page.getByLabel('Client Order Reference').fill('PAY-VALIDATION-001')
  await page.getByRole('button', { name: 'Create Payment Order' }).click()

  // Field-level validation message from the Zod schema (min: 1)
  await expect(page.getByText(/amount must be at least 1/i)).toBeVisible()

  // No POST must have been fired
  expect(postFired).toBe(false)
})

/**
 * An empty clientOrderReference violates z.string().min(1).
 * Validates: Requirement 3.3
 */
test('validation: empty clientOrderReference shows error and blocks submit', async ({ page }) => {
  await mockAuthenticatedSession(page)

  let postFired = false
  await page.route(createUrl, async (route) => {
    if (route.request().method() === 'POST') {
      postFired = true
    }
    await route.fallback()
  })

  await gotoCreatePage(page, merchantId)

  // Fill amount and currency but leave reference empty
  await page.getByLabel('Amount (minor units)').fill('5000')
  await page.getByLabel('Currency').click()
  await page.locator('[data-slot="itemLabel"]').filter({ hasText: 'EUR' }).click()
  // Intentionally leave Client Order Reference empty
  await page.getByRole('button', { name: 'Create Payment Order' }).click()

  await expect(page.getByText(/client order reference is required/i)).toBeVisible()
  expect(postFired).toBe(false)
})

/**
 * The currency field is a USelect constrained to PLN/EUR/USD.
 * Not selecting any currency should produce a validation error.
 * Validates: Requirement 3.3
 */
test('validation: no currency selected shows error and blocks submit', async ({ page }) => {
  await mockAuthenticatedSession(page)

  let postFired = false
  await page.route(createUrl, async (route) => {
    if (route.request().method() === 'POST') {
      postFired = true
    }
    await route.fallback()
  })

  await gotoCreatePage(page, merchantId)

  // Fill amount and reference but do NOT touch the currency select
  await page.getByLabel('Amount (minor units)').fill('9900')
  await page.getByLabel('Client Order Reference').fill('PAY-NO-CURRENCY')
  await page.getByRole('button', { name: 'Create Payment Order' }).click()

  // Zod 4 enum error: "Invalid option: expected one of 'PLN'|'EUR'|'USD'"
  // Zod 3 error code style: "invalid_enum_value" (kept for forward compat)
  await expect(page.getByText(/invalid_enum_value|invalid option|currency is required|select a currency/i)).toBeVisible()
  expect(postFired).toBe(false)
})

// ─── Req 3.4 — Idempotency-Key reuse on unchanged resubmit ──────────────────

/**
 * When the API returns a 400 failure:
 *  1. The form retains its values.
 *  2. The same Idempotency-Key is kept in the input.
 *  3. Resubmitting without changing any value sends the identical key.
 * Validates: Requirement 3.4
 */
test('idempotency key is reused on unchanged resubmit after 400 failure', async ({ page }) => {
  await mockAuthenticatedSession(page)

  const requests: { idempotencyKey: string }[] = []

  await page.route(createUrl, async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback()
      return
    }

    const key = route.request().headers()['idempotency-key'] ?? ''
    requests.push({ idempotencyKey: key })

    // Always return 400 so the form stays in error state
    await route.fulfill({
      status: 400,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        type: 'https://example.com/problems/validation',
        title: 'Validation Error',
        status: 400,
        detail: 'Simulated validation failure',
        instance: `/api/merchants/${merchantId}/payment-orders`,
      }),
    })
  })

  await gotoCreatePage(page, merchantId)

  // Capture the initial idempotency key before any submit
  const keyInput = page.getByTestId('idempotency-key-input').locator('input')
  await expect(keyInput).toBeVisible()
  const keyBeforeFirstSubmit = await keyInput.inputValue()
  expect(keyBeforeFirstSubmit).toBeTruthy()

  // Fill valid form values
  await page.getByLabel('Amount (minor units)').fill('7500')
  await page.getByLabel('Currency').click()
  await page.locator('[data-slot="itemLabel"]').filter({ hasText: 'USD' }).click()
  await page.getByLabel('Client Order Reference').fill('PAY-IDEMPOTENCY-TEST')

  // First submit → 400 failure
  await page.getByRole('button', { name: 'Create Payment Order' }).click()
  await expect(page.getByTestId('error-state')).toBeVisible()

  // Read the key after failure — must be unchanged
  const keyAfterFailure = await keyInput.inputValue()
  expect(keyAfterFailure).toBe(keyBeforeFirstSubmit)

  // Resubmit without changing any form values
  await page.getByRole('button', { name: 'Create Payment Order' }).click()
  await expect(page.getByTestId('error-state')).toBeVisible()

  // Both requests must have been captured and must share the same key
  expect(requests).toHaveLength(2)
  expect(requests[0].idempotencyKey).toBe(requests[1].idempotencyKey)
})

// ─── Req 3.9 — Empty state when payment order list is empty ─────────────────

/**
 * When the payment order list response has zero items the table must show the
 * EmptyStateCard (data-testid="empty-state") rather than an empty table.
 * Validates: Requirement 3.9
 */
test('payment order list shows empty state when there are no orders', async ({ page }) => {
  await mockAuthenticatedSession(page)

  // Mock summary to satisfy that panel
  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalOrders: 0,
        totalAmountMinor: 0,
        byCurrency: [],
        byStatus: [],
      }),
    })
  })

  // Mock list returning zero items (must match before summary route because it is a superset URL)
  await page.route(`**/api/merchants/${merchantId}/payment-orders**`, async (route) => {
    if (route.request().url().includes('/summary')) {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await dismissDevtoolsOverlay(page)

  // The EmptyStateCard must be visible inside the payment-order-table container
  await expect(page.getByTestId('payment-order-table')).toBeVisible({ timeout: 15000 })
  // Wait for loading to complete and empty state to appear
  // The EmptyStateCard renders "This merchant has no payment orders yet." when list is empty
  await expect(page.getByText(/no payment orders/i)).toBeVisible({ timeout: 15000 })

  // Table rows must not be rendered
  await expect(page.getByRole('row')).toHaveCount(0)
})
