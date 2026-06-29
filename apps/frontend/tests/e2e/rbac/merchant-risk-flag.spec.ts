/**
 * F-C6: Risk Flags RBAC-Gated Merchant Review (Phase 3B-5)
 *
 * Validates that the risk flag mutation control is shown/hidden based on role,
 * and that the risk badge appears for flagged merchants in the table.
 *
 * Playwright capabilities demonstrated:
 *   - mockRoleSession()          — typed multi-role session helper (F-A2)
 *   - expect.toBeVisible()       — assert element IS rendered
 *   - expect.not.toBeVisible()   — assert element NOT in DOM (v-if=false)
 *   - locator.filter()           — filter table row by text content
 *   - page.route() composition   — session mock + API mock per test
 *
 * Token safety: no JWT, no Bearer, no Authorization in test data.
 */

import { expect, test } from '@playwright/test'
import { mockRoleSession } from '../../support/auth-roles'

const MERCHANT_ID = 'bbbbbbbb-1111-4111-8111-111111111111'

function merchantDetailResponse(riskFlagged: boolean) {
  return {
    merchantId: MERCHANT_ID,
    merchantReference: 'RISK-TEST-001',
    displayName: 'Risk Test Merchant',
    status: 'ACTIVE',
    createdAt: '2026-06-29T10:00:00Z',
    updatedAt: '2026-06-29T10:00:00Z',
    riskFlagged,
  }
}

async function mockMerchantDetailApi(
  page: import('@playwright/test').Page,
  riskFlagged = false,
) {
  await page.route(`**/api/merchants/${MERCHANT_ID}`, route => {
    if (route.request().method() === 'HEAD') return route.fulfill({ status: 200 })
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(merchantDetailResponse(riskFlagged)),
    })
  })
}

async function mockMerchantsListApi(
  page: import('@playwright/test').Page,
  riskFlagged = false,
) {
  await page.route('**/api/merchants', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        merchants: [merchantDetailResponse(riskFlagged)],
      }),
    }),
  )
}

// ── PLATFORM_ADMIN sees risk panel and can toggle ────────────────────────────

test.describe('Risk flag — PLATFORM_ADMIN (F-C6)', () => {
  test('PLATFORM_ADMIN sees risk panel and toggle button for unflagged merchant', async ({ page }) => {
    await mockRoleSession(page, 'PLATFORM_ADMIN')
    await mockMerchantDetailApi(page, false)

    await page.goto(`/admin/merchants/${MERCHANT_ID}`)

    const riskPanel = page.locator('[data-testid="merchant-risk-panel"]')
    await expect(riskPanel).toBeVisible()

    const riskStatus = page.locator('[data-testid="merchant-risk-status"]')
    await expect(riskStatus).toBeVisible()
    await expect(riskStatus).toContainText('No risk flag')

    const riskToggle = page.locator('[data-testid="merchant-risk-toggle"]')
    await expect(riskToggle).toBeVisible()
    await expect(riskToggle).toContainText('Mark as risk flagged')
  })

  test('PLATFORM_ADMIN sees risk flagged status for already-flagged merchant', async ({ page }) => {
    await mockRoleSession(page, 'PLATFORM_ADMIN')
    await mockMerchantDetailApi(page, true)

    await page.goto(`/admin/merchants/${MERCHANT_ID}`)

    const riskStatus = page.locator('[data-testid="merchant-risk-status"]')
    await expect(riskStatus).toBeVisible()
    await expect(riskStatus).toContainText('Risk flagged')

    const riskToggle = page.locator('[data-testid="merchant-risk-toggle"]')
    await expect(riskToggle).toBeVisible()
    await expect(riskToggle).toContainText('Clear risk flag')
  })
})

// ── MERCHANT_MANAGER cannot mutate risk flag ─────────────────────────────────

test.describe('Risk flag — MERCHANT_MANAGER cannot mutate (F-C6)', () => {
  test('MERCHANT_MANAGER does not see risk toggle button', async ({ page }) => {
    await mockRoleSession(page, 'MERCHANT_MANAGER')
    await mockMerchantDetailApi(page, false)

    await page.goto(`/admin/merchants/${MERCHANT_ID}`)

    // Risk panel is always visible (shows read-only status to any logged-in user)
    const riskPanel = page.locator('[data-testid="merchant-risk-panel"]')
    await expect(riskPanel).toBeVisible()

    // Toggle button is RBAC-gated — MERCHANT_MANAGER must not see it
    const riskToggle = page.locator('[data-testid="merchant-risk-toggle"]')
    await expect(riskToggle).not.toBeVisible()
  })
})

// ── SUPPORT_AGENT cannot mutate risk flag ────────────────────────────────────

test.describe('Risk flag — SUPPORT_AGENT cannot mutate (F-C6)', () => {
  test('SUPPORT_AGENT does not see risk toggle button', async ({ page }) => {
    await mockRoleSession(page, 'SUPPORT_AGENT')
    await mockMerchantDetailApi(page, true)

    await page.goto(`/admin/merchants/${MERCHANT_ID}`)

    const riskStatus = page.locator('[data-testid="merchant-risk-status"]')
    await expect(riskStatus).toBeVisible()
    await expect(riskStatus).toContainText('Risk flagged')

    // SUPPORT_AGENT can see status but cannot mutate
    const riskToggle = page.locator('[data-testid="merchant-risk-toggle"]')
    await expect(riskToggle).not.toBeVisible()
  })
})

// ── Merchant table risk badge — locator.filter ───────────────────────────────

test.describe('Merchant table risk badge — locator.filter (F-C6)', () => {
  test('risk badge is visible in table row for flagged merchant', async ({ page }) => {
    await mockRoleSession(page, 'PLATFORM_ADMIN')
    await mockMerchantsListApi(page, true)

    await page.goto('/admin/merchants')

    // Playwright capability: locator.filter() to find the row by merchant reference text
    const rows = page.locator('table tbody tr, [role="row"]')
    const riskRow = rows.filter({ hasText: 'Risk Test Merchant' })
    await expect(riskRow).toBeVisible()

    const riskBadge = riskRow.locator('[data-testid="merchant-risk-badge"]')
    await expect(riskBadge).toBeVisible()
    await expect(riskBadge).toContainText('Risk flagged')
  })

  test('no risk badge visible in table row for non-flagged merchant', async ({ page }) => {
    await mockRoleSession(page, 'PLATFORM_ADMIN')
    await mockMerchantsListApi(page, false)

    await page.goto('/admin/merchants')

    const rows = page.locator('table tbody tr, [role="row"]')
    const normalRow = rows.filter({ hasText: 'Risk Test Merchant' })
    await expect(normalRow).toBeVisible()

    const riskBadge = normalRow.locator('[data-testid="merchant-risk-badge"]')
    await expect(riskBadge).not.toBeVisible()
  })
})
