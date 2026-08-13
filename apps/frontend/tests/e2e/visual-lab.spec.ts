import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

test.describe('Visual Lab tiles', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedSession(page)
    await page.goto('/admin/merchants')
    await expect(page.getByTestId('nav-link-mirror-lab')).toBeVisible({ timeout: 15_000 })
    await page.getByTestId('nav-link-mirror-lab').click()
    await page.getByTestId('mirror-lab-open-visual').click()
    await expect(page.getByTestId('visual-tile-merchant-badge')).toBeVisible()
  })

  for (const tile of [
    'visual-tile-merchant-badge',
    'visual-tile-payment-badge',
    'visual-tile-problem-details',
    'visual-tile-hosted-cta',
    'visual-tile-idle-lock',
    'visual-tile-dark',
    'visual-tile-expired',
  ]) {
    test(`screenshot ${tile}`, async ({ page }) => {
      await expect(page.getByTestId(tile)).toHaveScreenshot(`${tile}.png`, {
        stylePath: 'tests/e2e/visual-lab-mask.css',
      })
    })
  }

  test('break visual is tagged and not default CI', { tag: '@visual-negative' }, async ({ page }) => {
    await page.getByTestId('visual-lab-break').click()
    await expect(page.getByTestId('visual-tile-hosted-cta')).toHaveScreenshot('visual-tile-hosted-cta-broken.png', {
      stylePath: 'tests/e2e/visual-lab-mask.css',
    })
  })
})
