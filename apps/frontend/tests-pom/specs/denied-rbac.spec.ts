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

  test('GET /api/search is 403 not 401 or 502', { tag: ['@security'] }, async ({ page }) => {
    const response = await page.request.get('/api/search?q=denied-search')
    expect(response.status()).toBe(403)
    expect(response.status()).not.toBe(401)
    expect(response.status()).not.toBe(502)
    expectProblem(await response.json(), 403)
  })

  test('deep-link /admin/merchants is denied without a merchant table', { tag: ['@security'] }, async ({ app }) => {
    await app.merchants.goto()
    await app.merchants.expectAccessDenied()
  })

  test('PW-OPS-E2E-203 denied search does not request entities and static navigation works', { tag: ['@security'] }, async ({ app, page }) => {
    await app.merchants.goto()
    await app.commandPalette.openFromButton()
    let entitySearchRequests = 0
    const onRequest = (request: { method: () => string, url: () => string }) => {
      if (request.method() === 'GET') {
        try {
          if (new URL(request.url()).pathname === '/api/search') {
            entitySearchRequests += 1
          }
        }
        catch {
          // Ignore browser-internal request URLs.
        }
      }
    }
    page.on('request', onRequest)
    try {
      await app.commandPalette.search('Error Lab')
      await app.commandPalette.selectOptionInGroup('Go to', 'Error Lab')
      await expect(app.page).toHaveURL(/\/error-lab$/)
      expect(entitySearchRequests).toBe(0)
      await expect(app.merchants.developerErrorOverlay()).toHaveCount(0)
    }
    finally {
      page.off('request', onRequest)
    }
  })

  test('forbidden page matches ARIA snapshot', { tag: ['@a11y'] }, async ({ app }) => {
    await app.forbidden.goto()
    await app.forbidden.expectLoaded()
    await expect(app.forbidden.heading()).toBeFocused()
    await expect(app.forbidden.root()).toMatchAriaSnapshot()
  })
})
