import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession, mockMerchantApi } from './merchant-support'
import type { Merchant } from './merchant-support'

/**
 * F-D5 — Visual regression for status badges.
 *
 * Playwright capability demonstrated: toHaveScreenshot() — pixel-level
 * comparison against a committed baseline. First visual-regression coverage
 * in this repo (no toHaveScreenshot() usage/config existed before this
 * phase). Complements F-D4's ARIA snapshots: ARIA snapshots catch structural/
 * accessibility regressions, screenshots catch visual ones (wrong color,
 * missing border, broken variant) that an accessibility tree can't see.
 *
 * Scoped to individual badge locators, not full pages — small, stable
 * targets keep the baseline meaningful and avoid unrelated layout noise
 * causing false failures. Baselines are committed under
 * tests/e2e/visual-regression.spec.ts-snapshots/.
 */

const visualMerchants: Merchant[] = [
  {
    merchantId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    merchantReference: 'MERCH-VISUAL-001',
    displayName: 'MERCH-VISUAL-001 Display',
    status: 'PENDING',
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  },
  {
    merchantId: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    merchantReference: 'MERCH-VISUAL-002',
    displayName: 'MERCH-VISUAL-002 Display',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  },
  {
    merchantId: 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee',
    merchantReference: 'MERCH-VISUAL-003',
    displayName: 'MERCH-VISUAL-003 Display',
    status: 'SUSPENDED',
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  },
]

test.describe('BusinessStatusBadge visual regression (F-D5)', () => {
  // Targets the merchant detail page (`[merchantId]/index.vue`), which renders
  // BusinessStatusBadge directly via `data-testid="merchant-status-badge"`.
  // Note: the merchant LIST table (MerchantTable.vue) does NOT use
  // BusinessStatusBadge — it renders its own inline UBadge with raw
  // uppercase status text via a local statusColor() helper. The detail page
  // is the correct target for this component specifically.
  for (const [label, merchant] of [
    ['Pending (warning)', visualMerchants[0]!],
    ['Active (success)', visualMerchants[1]!],
    ['Suspended (error)', visualMerchants[2]!],
  ] as const) {
    test(`merchant status badge — ${label}`, async ({ page }) => {
      await mockAuthenticatedSession(page)
      await mockMerchantApi(page, [merchant])

      await page.goto(`/admin/merchants/${merchant.merchantId}`)
      await expect(page.getByTestId('merchant-detail-panel')).toBeVisible({ timeout: 15000 })

      const badge = page.getByTestId('merchant-status-badge')
      await expect(badge).toBeVisible()
      await expect(badge).toHaveScreenshot(`merchant-badge-${merchant.status.toLowerCase()}.png`)
    })
  }
})

test.describe('HttpStatusBadge visual regression (F-D5)', () => {
  /**
   * Reuses the existing Error Lab 429 flow (F-A3) — already mocked, no
   * backend needed — as the vehicle to render a live HttpStatusBadge.
   */
  test('HTTP status badge — 429 Client Error', async ({ page }) => {
    await page.route('**/api/_auth/session', route =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ loggedIn: true, user: { username: 'platform.operator' } }),
      }),
    )
    await page.goto('/admin/merchants')
    await expect(page.getByTestId('nav-link-error-lab')).toBeVisible({ timeout: 15000 })
    await page.getByTestId('nav-link-error-lab').click()

    await expect(page.getByTestId('error-lab-trigger-429')).toBeVisible({ timeout: 15000 })
    await page.getByTestId('error-lab-trigger-429').click()

    const badge = page.getByTestId('problem-details-card').getByText('429 Client Error')
    await expect(badge).toBeVisible({ timeout: 10000 })
    await expect(badge).toHaveScreenshot('http-status-badge-429.png')
  })
})
