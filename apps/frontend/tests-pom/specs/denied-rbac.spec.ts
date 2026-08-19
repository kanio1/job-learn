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

  test('forbidden page matches ARIA snapshot', { tag: ['@a11y'] }, async ({ app }) => {
    await app.page.goto('/forbidden')
    await expect(app.page.getByTestId('forbidden-page')).toBeVisible()
    await expect(app.page.getByTestId('forbidden-page')).toMatchAriaSnapshot()
  })
})
