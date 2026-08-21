import { pomAuthFiles } from '../utils/env'
import { test, expect } from '../fixtures'
import { expectProblem } from '../utils/http'

test.describe('merchant.denied RBAC', () => {
  test.use({ storageState: pomAuthFiles.merchantDenied })

  test('GET /api/merchants is 403 not 401 or 502', { tag: ['@security'] }, async ({ page }) => {
    const response = await page.request.get('/api/merchants')
    expect(response.status()).toBe(403)
    expect(response.status()).not.toBe(401)
    expect(response.status()).not.toBe(502)
    expectProblem(await response.json(), 403)
  })

  test('deep-link /admin/merchants is denied without a merchant table', { tag: ['@security'] }, async ({ app }) => {
    await app.merchants.goto()
    await app.merchants.expectAccessDenied()
  })

  test('PW-OPS-E2E-203 denied search 403 does not crash palette', { tag: ['@security'] }, async ({ app, page }) => {
    await app.page.goto('/admin/merchants')
    await app.commandPalette.openFromButton()
    const search = page.waitForResponse((response) => {
      try {
        return new URL(response.url()).pathname === '/api/search'
      }
      catch {
        return false
      }
    })
    await app.commandPalette.search('denied-search')
    const status = (await search).status()
    expect([403, 200]).toContain(status)
    await expect(app.commandPalette.dialog()).toBeVisible()
    await expect(app.page.locator('vite-plugin-checker-error-overlay')).toHaveCount(0)
  })

  test('forbidden page matches ARIA snapshot', { tag: ['@a11y'] }, async ({ app }) => {
    await app.page.goto('/forbidden')
    await expect(app.page.getByTestId('forbidden-page')).toBeVisible()
    await expect(app.page.getByTestId('forbidden-page')).toMatchAriaSnapshot()
  })
})
