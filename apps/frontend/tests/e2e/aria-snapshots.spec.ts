import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession, mockMerchantApi } from './merchant-support'
import type { Merchant } from './merchant-support'

/**
 * F-D4 — ARIA snapshot testing on existing pages.
 *
 * Playwright capability demonstrated: toMatchAriaSnapshot() — a semantic,
 * accessibility-tree-based assertion. Unlike toHaveScreenshot() (pixels) it
 * asserts roles/names/structure, so it survives CSS/theme changes but still
 * catches accessibility regressions (a button losing its accessible name, a
 * table losing a column, a form field losing its label).
 *
 * No frontend code change needed — these are read-only structural snapshots
 * of pages that already exist. Snapshot files are committed under
 * tests/e2e/aria-snapshots.spec.ts-snapshots/ on first run.
 */

/** Dismiss the Nuxt devtools error overlay if it appears (caused by pre-existing TS errors) */
async function dismissDevtoolsOverlay(page: import('@playwright/test').Page) {
  const closeBtn = page.getByRole('button', { name: 'Close' })
  if (await closeBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
    await closeBtn.click()
    await closeBtn.waitFor({ state: 'hidden', timeout: 3000 }).catch(() => {})
  }
}

// Fixed IDs/timestamps — an ARIA snapshot asserts exact accessible names, so
// the mocked data must be deterministic across runs (no randomUUID(), no
// `new Date()`), unlike the randomized `merchant()` helper used elsewhere.
const ariaMerchants: Merchant[] = [
  {
    merchantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
    merchantReference: 'MERCH-ARIA-001',
    displayName: 'MERCH-ARIA-001 Display',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  },
  {
    merchantId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
    merchantReference: 'MERCH-ARIA-002',
    displayName: 'MERCH-ARIA-002 Display',
    status: 'SUSPENDED',
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  },
]

test('merchant table matches ARIA snapshot (F-D4)', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, ariaMerchants)

  await page.goto('/admin/merchants')
  await dismissDevtoolsOverlay(page)
  await expect(page.getByRole('heading', { name: 'Merchants' })).toBeVisible({ timeout: 15000 })

  const table = page.getByRole('table')
  await expect(table).toBeVisible()
  await expect(table).toMatchAriaSnapshot()
})

test('payment order create form matches ARIA snapshot (F-D4)', async ({ page }) => {
  const merchantId = '11111111-1111-4111-8111-111111111111'

  await page.route('**/api/_auth/session', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ loggedIn: true, user: { username: 'platform.operator', roles: ['MERCHANT_MANAGER'] } }),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/new`)
  await expect(page.getByRole('heading', { name: 'New Payment Order' })).toBeVisible({ timeout: 15000 })
  await dismissDevtoolsOverlay(page)
  await expect(page.getByRole('button', { name: 'Create Payment Order' })).toBeVisible({ timeout: 15000 })

  const form = page.getByTestId('create-payment-order-form')
  await expect(form).toMatchAriaSnapshot()
})
