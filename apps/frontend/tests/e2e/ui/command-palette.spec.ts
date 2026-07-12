import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from '../merchant-support'

/**
 * F-D3 — Command palette (Ctrl+K).
 *
 * The command palette itself (`UDashboardSearch`/`UDashboardSearchButton` in
 * `app/layouts/dashboard.vue`, from the Nuxt Dashboard Template) already
 * exists and is already covered by a click-triggered open/close test
 * (F-B4, `tests/e2e/ui/confirm-action-modal.spec.ts`). What F-D3 actually
 * adds, per the roadmap's stated unlock, is the KEYBOARD-shortcut trigger
 * and keyboard-driven navigation — no frontend code change needed.
 *
 * Playwright capabilities demonstrated:
 *   - page.keyboard.press('Control+k')  — global keyboard shortcut trigger
 *   - page.keyboard.type()              — type into the palette's search input
 *   - page.keyboard.press('Enter')      — select the highlighted result
 *   - toMatchAriaSnapshot()             — structural assertion on the open palette
 */

test.describe('Command palette (F-D3)', () => {
  test('Ctrl+K opens the command palette', async ({ page }) => {
    await mockAuthenticatedSession(page)
    await page.goto('/admin/merchants')
    await expect(page.getByRole('heading', { name: 'Merchants' })).toBeVisible({ timeout: 15000 })

    // Palette is closed initially.
    await expect(page.getByText('Search Payment Quality Lab')).not.toBeVisible()

    await page.keyboard.press('Control+k')
    await expect(page.getByText('Search Payment Quality Lab')).toBeVisible({ timeout: 10000 })

    await page.keyboard.press('Escape')
    await expect(page.getByText('Search Payment Quality Lab')).not.toBeVisible()
  })

  test('Ctrl+K then typing and Enter navigates via keyboard only', async ({ page }) => {
    await mockAuthenticatedSession(page)
    await page.goto('/admin/merchants')
    await expect(page.getByRole('heading', { name: 'Merchants' })).toBeVisible({ timeout: 15000 })

    await page.keyboard.press('Control+k')
    await expect(page.getByText('Search Payment Quality Lab')).toBeVisible({ timeout: 10000 })

    // "Error Lab" is always visible regardless of role — a stable target. It
    // matches two palette entries ("Go to" nav link + an "Actions" entry),
    // both pointing at the same /error-lab href, so the test doesn't need to
    // care which one ends up highlighted — only that typing narrows the
    // list down to just those two before a keyboard selection is made.
    await page.keyboard.type('Error Lab')
    await expect(page.getByRole('option')).toHaveCount(2, { timeout: 10000 })

    // Typing narrows the list but doesn't move keyboard focus onto the new
    // top result on its own — move it explicitly before selecting, the same
    // way a real keyboard-only user would.
    await page.keyboard.press('ArrowDown')
    await page.keyboard.press('Enter')

    // Palette closes and the app navigates — no mouse interaction at any point.
    await expect(page.getByText('Search Payment Quality Lab')).not.toBeVisible()
    await expect(page).toHaveURL(/\/error-lab$/)
  })

  test('open command palette matches ARIA snapshot', async ({ page }) => {
    await mockAuthenticatedSession(page)
    await page.goto('/admin/merchants')
    await expect(page.getByRole('heading', { name: 'Merchants' })).toBeVisible({ timeout: 15000 })

    await page.keyboard.press('Control+k')
    await expect(page.getByText('Search Payment Quality Lab')).toBeVisible({ timeout: 10000 })

    await expect(page.getByRole('dialog')).toMatchAriaSnapshot()
  })
})
