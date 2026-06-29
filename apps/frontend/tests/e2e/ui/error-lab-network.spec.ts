/**
 * F-A3: Network Header Assertion Patterns — Error Lab 429 UI + network tests
 * F-D6: Console/PageError Monitoring + browser storage token guard
 *
 * Playwright capabilities demonstrated:
 *   - page.waitForResponse()   — intercept a real BFF network response
 *   - response.status()        — assert HTTP status from the network layer
 *   - response.headers()       — assert response headers at the network layer
 *   - page.evaluate()          — check localStorage/sessionStorage
 *   - page.on('console')       — console.error guard
 *   - page.on('pageerror')     — uncaught JS error guard
 *   - getByTestId()            — UI layer assertions with stable test selectors
 *
 * Auth: mocked via page.route for the Nuxt session API endpoint
 * Backend: NOT required — trigger-429 is a standalone BFF mock (no backend call)
 *
 * Error Lab 304, trigger-401, trigger-428, idempotency-replay require auth session +
 * running backend. They are deferred to Phase 3A-4 when multi-role auth is ready.
 */

import { expect, test } from '@playwright/test'
import { attachConsoleErrorGuard, expectNoTokenInBrowserStorage } from '../../support/browser-safety-assertions'
import { expectNoAuthorizationInNetworkResponse, expectRetryAfterHeader } from '../../support/network-assertions'

/**
 * Navigate to the Error Lab page via the dashboard sidebar.
 *
 * WHY: /error-lab is server-side rendered (SSR). page.route() only intercepts
 * browser-side requests, not the server-to-itself session check during SSR.
 * /admin/** pages have ssr:false — they boot as SPA, so page.route() mocks
 * the session check during client-side boot.
 *
 * Solution: land on /admin/merchants (SPA), then click the always-visible
 * nav-link-error-lab in the sidebar to do client-side navigation to /error-lab.
 */
async function gotoErrorLabViaSidebar(page: import('@playwright/test').Page): Promise<void> {
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
}

const TRIGGER_429_URL = '**/api/error-lab/trigger-429'

test.describe('Error Lab — 429 network + UI (F-A3)', () => {
  /**
   * page.waitForResponse() captures the live BFF response BEFORE the UI updates.
   * Key learning: set up the promise synchronously, then trigger the network call.
   *
   * This test verifies the HTTP layer contract:
   *   - status 429
   *   - Retry-After header is a positive integer
   *   - Authorization header is NOT present in the response
   */
  test('page.waitForResponse captures 429 status and Retry-After header', async ({ page }) => {
    await gotoErrorLabViaSidebar(page)
    await expect(page.getByTestId('error-lab-trigger-429')).toBeVisible({ timeout: 15000 })

    // Set up the promise BEFORE clicking — this is the correct page.waitForResponse() pattern.
    const responsePromise = page.waitForResponse(TRIGGER_429_URL)
    await page.getByTestId('error-lab-trigger-429').click()
    const response = await responsePromise

    // Network layer assertions
    expect(response.status(), 'BFF must return HTTP 429').toBe(429)
    const seconds = expectRetryAfterHeader(response)
    expect(seconds, 'Retry-After from trigger-429 is always 30').toBe(30)
    expectNoAuthorizationInNetworkResponse(response)
  })

  /**
   * After the network response, the UI must render the ProblemDetailsCard
   * with the retryable extension fields.
   *
   * This test combines page.waitForResponse() (network layer) with
   * getByTestId() assertions (UI layer), verifying they stay in sync.
   */
  test('UI shows retryable badge and retryAfterSeconds after 429 trigger', async ({ page }) => {
    const getErrors = attachConsoleErrorGuard(page)

    await gotoErrorLabViaSidebar(page)
    await expect(page.getByTestId('error-lab-trigger-429')).toBeVisible({ timeout: 15000 })

    const responsePromise = page.waitForResponse(TRIGGER_429_URL)
    await page.getByTestId('error-lab-trigger-429').click()
    await responsePromise

    // UI layer: ProblemDetailsCard should render with retryable extension
    await expect(page.getByTestId('problem-details-card')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('retryable-value')).toContainText('Yes')

    // UI layer: response headers panel (first occurrence, before ApiDebugPanel)
    // should show Retry-After. Note: 'http-headers-panel' appears twice — once
    // for response headers (this one) and once inside ApiDebugPanel for request headers.
    await expect(page.getByTestId('http-headers-panel').first()).toContainText('Retry-After')

    // F-D6: no unexpected console errors.
    // Allowlist:
    //   - '[nuxt]', '[Vue warn]', 'vite-plugin-checker' — framework noise
    //   - 'status of 429' — intentional: we triggered a 429 in this very test
    //   - 'status of 401' — expected in test env: /admin/merchants API call fails
    //     (backend not running), not an Error Lab regression
    const errors = getErrors().filter(
      e =>
        !e.includes('[nuxt]') &&
        !e.includes('[Vue warn]') &&
        !e.includes('vite-plugin-checker') &&
        !e.includes('status of 429') &&
        !e.includes('status of 401'),
    )
    expect(errors, 'No unexpected console errors should occur during Error Lab interaction').toHaveLength(0)
  })
})

test.describe('Browser storage token guard (F-D6)', () => {
  /**
   * The BFF uses server-side sealed session cookies — the access token must NEVER
   * reach localStorage or sessionStorage.
   *
   * Playwright capability: page.evaluate() — run arbitrary JS in the page context.
   * This test validates the sealed session architecture from the browser's perspective.
   */
  test('localStorage and sessionStorage contain no JWT or Bearer token', async ({ page }) => {
    await gotoErrorLabViaSidebar(page)
    await expect(page.getByTestId('error-lab-trigger-429')).toBeVisible({ timeout: 15000 })

    // Trigger a real BFF call to ensure any auth-related storage writes have happened
    const responsePromise = page.waitForResponse(TRIGGER_429_URL)
    await page.getByTestId('error-lab-trigger-429').click()
    await responsePromise

    await expectNoTokenInBrowserStorage(page)
  })
})
