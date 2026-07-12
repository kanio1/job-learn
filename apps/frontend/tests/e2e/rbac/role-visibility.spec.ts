/**
 * F-A2: Multi-role UI visibility comparison (Phase 3A-4)
 *
 * Validates that the frontend's RBAC-gated elements are shown or hidden
 * correctly for different composite roles. Tests use the mock-session
 * pattern (page.route) — no real Keycloak, no token logging.
 *
 * Role capabilities tested (source: ~/utils/rbacMatrix.ts):
 *   PLATFORM_ADMIN:  canCreateMerchant=true, canUpdateMerchantStatus=true
 *   SUPPORT_AGENT:   canCreateMerchant=false, canUpdateMerchantStatus=false
 *
 * Playwright capabilities demonstrated:
 *   - mockRoleSession()          — typed multi-role session helper (F-A2)
 *   - expect.toBeVisible()       — assert element IS rendered
 *   - expect.not.toBeVisible()   — assert element NOT in DOM (v-if=false)
 *   - page.route() composition   — session mock + API mock per test
 *
 * Token safety: no JWT, no Bearer, no Authorization in test data.
 */

import { expect, test } from '@playwright/test'
import { mockRoleSession } from '../../support/auth-roles'

// Valid RFC 4122 UUID (Zod 4 enforces variant nibble [89abAB])
const merchantId = '11111111-1111-4111-8111-111111111111'

// ── Shared API mocks ─────────────────────────────────────────────────────────

async function mockMerchantsListApi(page: import('@playwright/test').Page) {
  await page.route('**/api/merchants', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ merchants: [] }),
    }),
  )
}

async function mockMerchantDetailApi(
  page: import('@playwright/test').Page,
  id: string,
  // Schema: z.enum(['DRAFT', 'ACTIVE', 'SUSPENDED']) — matches the real backend
  // MerchantStatus.java enum (TD-2B; previously this mock and the schema both
  // incorrectly used PENDING instead of DRAFT).
  status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' = 'DRAFT',
) {
  await page.route(`**/api/merchants/${id}`, route => {
    if (route.request().method() === 'HEAD') {
      return route.fulfill({ status: 200 })
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        merchantId: id,
        merchantReference: 'TEST-RBAC-001',
        displayName: 'RBAC Test Merchant',
        status,
        createdAt: '2026-06-29T10:00:00Z',
        updatedAt: '2026-06-29T10:00:00Z',
      }),
    })
  })
}

// ── Merchant list — create action gating ────────────────────────────────────

test.describe('Merchant create button — canCreateMerchant gating (F-A2)', () => {
  /**
   * PLATFORM_ADMIN has canCreateMerchant=true.
   * The "New Merchant" button (data-testid="action-create-merchant") must be
   * visible inside the dashboard navbar right slot.
   *
   * Key: canCreateMerchant is computed client-side from session.user.roles
   * with no extra API call. The button appears once the session is fetched.
   */
  test('PLATFORM_ADMIN sees create merchant button', async ({ page }) => {
    await mockRoleSession(page, 'PLATFORM_ADMIN')
    await mockMerchantsListApi(page)

    await page.goto('/admin/merchants')
    // Search button confirms the dashboard shell rendered (always visible in header)
    await expect(page.getByRole('button', { name: /Search/ })).toBeVisible({ timeout: 15000 })

    await expect(page.getByTestId('action-create-merchant')).toBeVisible()
  })

  /**
   * SUPPORT_AGENT has canCreateMerchant=false.
   * The create button must be absent from the DOM (v-if removes it entirely).
   * We wait for the page shell to confirm rendering completed before the assertion.
   */
  test('SUPPORT_AGENT does not see create merchant button', async ({ page }) => {
    await mockRoleSession(page, 'SUPPORT_AGENT')
    await mockMerchantsListApi(page)

    await page.goto('/admin/merchants')
    // Search button confirms the page rendered — not conditional on canCreateMerchant
    await expect(page.getByRole('button', { name: /Search/ })).toBeVisible({ timeout: 15000 })

    // v-if="!insufficientAuthority && canCreateMerchant" removes element from DOM
    await expect(page.getByTestId('action-create-merchant')).not.toBeVisible()
  })
})

// ── Merchant detail — status actions card gating ─────────────────────────────

test.describe('Merchant status actions — canUpdateMerchantStatus gating (F-A2)', () => {
  /**
   * PLATFORM_ADMIN has canUpdateMerchantStatus=true.
   * A DRAFT merchant should show the Activate button inside the Status Actions card
   * (v-if="can.canUpdateMerchantStatus" on the UCard wrapper).
   */
  test('PLATFORM_ADMIN sees activate button on DRAFT merchant detail', async ({ page }) => {
    await mockRoleSession(page, 'PLATFORM_ADMIN')
    await mockMerchantDetailApi(page, merchantId, 'DRAFT')

    await page.goto(`/admin/merchants/${merchantId}`)
    // Wait for merchant data to render — data-testid="merchant-name" is inside the info card
    await expect(page.getByTestId('merchant-name')).toBeVisible({ timeout: 15000 })

    // Status Actions card is visible + merchant is DRAFT → Activate button present
    await expect(page.getByTestId('action-activate-merchant')).toBeVisible()
  })

  /**
   * SUPPORT_AGENT has canUpdateMerchantStatus=false.
   * The entire Status Actions UCard is removed from the DOM by v-if.
   * The Activate button is therefore never rendered regardless of merchant status.
   */
  test('SUPPORT_AGENT does not see activate button on DRAFT merchant detail', async ({ page }) => {
    await mockRoleSession(page, 'SUPPORT_AGENT')
    await mockMerchantDetailApi(page, merchantId, 'DRAFT')

    await page.goto(`/admin/merchants/${merchantId}`)
    // Wait for merchant data to render — proves page loaded before the negative assertion
    await expect(page.getByTestId('merchant-name')).toBeVisible({ timeout: 15000 })

    // v-if="can.canUpdateMerchantStatus" = false → UCard removed → button absent
    await expect(page.getByTestId('action-activate-merchant')).not.toBeVisible()
  })
})
