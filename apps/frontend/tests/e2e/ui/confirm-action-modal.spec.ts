/**
 * F-B4: DOM Modal Lifecycle — dashboard search modal open/close E2E test
 *
 * The originally planned target (ConfirmActionModal on the payment order detail page)
 * was blocked by a pre-existing Nuxt routing issue (Phase 3A-2). That routing bug
 * has been fixed in Phase 3A-2.5: [merchantId].vue was removed and its content moved
 * to [merchantId]/index.vue, eliminating the automatic nesting that made child routes
 * (/payments/new, /payments/:id) inaccessible.
 *
 * This test uses the global dashboard search modal (triggered by Ctrl+K or the
 * search button) which is accessible from any /admin/** page. It demonstrates the
 * same Playwright DOM modal lifecycle capabilities as the ConfirmActionModal target:
 *   - open modal via button click or keyboard shortcut
 *   - assert modal content text is visible (not just a wrapper testid)
 *   - close modal via keyboard (Escape)
 *   - assert modal content is no longer visible
 *
 * Playwright capabilities demonstrated:
 *   - page.route()           — mock session for /admin/** SPA navigation
 *   - page.getByRole()       — click the search button by accessible role+name
 *   - page.keyboard.press()  — close modal via Escape key
 *   - .toBeVisible()         — assert modal content appeared
 *   - .not.toBeVisible()     — assert modal content disappeared after close
 *   - page.evaluate()        — check browser storage for tokens (F-D6 combo)
 *
 * When to add ConfirmActionModal test:
 *   The payment order detail page now renders correctly (routing fix is done).
 *   Phase 3A-3+ can target ConfirmActionModal directly via the Cancel/Authorize buttons
 *   on the payment order detail page at /admin/merchants/:id/payments/:pid.
 */

import { expect, test } from '@playwright/test'
import { expectNoTokenInBrowserStorage } from '../../support/browser-safety-assertions'

async function mockAdminSession(page: import('@playwright/test').Page): Promise<void> {
  await page.route('**/api/_auth/session', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ loggedIn: true, user: { username: 'platform.operator' } }),
    }),
  )
}

test.describe('Dashboard search modal DOM lifecycle (F-B4)', () => {
  /**
   * Flow: click Search button → modal opens → Escape → modal closes
   *
   * The search modal (UCommandPalette inside UModal) is accessible from every
   * admin page. Its content text ('Search Payment Quality Lab') only renders when
   * the modal is open — this is the correct pattern: assert text visibility,
   * not the presence of a wrapper element that may always be in the DOM.
   *
   * Key learning: asserting .not.toBeVisible() after close confirms the modal
   * portal unmounted (or became hidden) — use this pattern for ConfirmActionModal
   * once the payment order routing fix is applied.
   */
  test('search button opens modal and Escape closes it', async ({ page }) => {
    await mockAdminSession(page)
    await page.goto('/admin/merchants')

    // Step 1: open the search modal via the header button
    const searchBtn = page.getByRole('button', { name: /Search/ })
    await expect(searchBtn).toBeVisible({ timeout: 15000 })
    await searchBtn.click()

    // Step 2: modal content is visible — text only renders when modal is open
    await expect(page.getByText('Search Payment Quality Lab')).toBeVisible({ timeout: 10000 })

    // Step 3: close via Escape key
    await page.keyboard.press('Escape')

    // Step 4: modal content is gone
    await expect(page.getByText('Search Payment Quality Lab')).not.toBeVisible()
  })

  /**
   * Combo test: modal open/close + browser storage token guard (F-D6).
   *
   * After opening and closing the search modal, browser storage must still
   * contain no JWT or Bearer token. The interaction with the search modal
   * makes no network calls that could cause accidental token exposure.
   *
   * Playwright capability: page.evaluate() — run JS in the page context.
   */
  test('no JWT in browser storage after search modal interaction', async ({ page }) => {
    await mockAdminSession(page)
    await page.goto('/admin/merchants')

    const searchBtn = page.getByRole('button', { name: /Search/ })
    await expect(searchBtn).toBeVisible({ timeout: 15000 })
    await searchBtn.click()
    await expect(page.getByText('Search Payment Quality Lab')).toBeVisible({ timeout: 10000 })
    await page.keyboard.press('Escape')
    await expect(page.getByText('Search Payment Quality Lab')).not.toBeVisible()

    await expectNoTokenInBrowserStorage(page)
  })
})
