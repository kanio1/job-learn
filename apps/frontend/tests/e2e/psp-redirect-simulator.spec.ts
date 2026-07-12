import { expect, test } from '@playwright/test'

/**
 * F-D2 — PSP Redirect Simulator.
 *
 * A standalone, unauthenticated (see auth.global.ts) mock PSP checkout page
 * opened in a new browser tab from Error Lab — demonstrates the multi-tab
 * redirect handoff pattern real card/3DS checkouts use, without any real
 * PSP integration, card fields, or PAN data (explicitly out of scope).
 *
 * Playwright capabilities demonstrated:
 *   - context.waitForPage()  — capture a new tab opened via target="_blank"
 *   - multi-tab coordination — interact with two Page objects in one test
 *   - page.close()           — explicitly close the second tab
 */

async function gotoErrorLab(page: import('@playwright/test').Page) {
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
  await expect(page.getByTestId('psp-redirect-trigger')).toBeVisible({ timeout: 15000 })
}

test('opens PSP simulator in a new tab, approves, and returns (F-D2)', async ({ page, context }) => {
  await gotoErrorLab(page)

  const newPagePromise = context.waitForEvent('page')
  await page.getByTestId('psp-redirect-trigger').click()
  const pspPage = await newPagePromise
  // 'networkidle' (not the default 'load') so the click below lands after
  // Vue hydration has actually attached event listeners — this is a fresh
  // dev-server route compile, and SSR-rendered HTML is visible/"actionable"
  // to Playwright before hydration finishes wiring @click handlers.
  await pspPage.waitForLoadState('networkidle')

  expect(context.pages()).toHaveLength(2)

  await expect(pspPage.getByTestId('psp-redirect-simulator')).toBeVisible()
  await expect(pspPage.getByTestId('psp-approve')).toBeVisible()
  await expect(pspPage.getByTestId('psp-decline')).toBeVisible()

  await pspPage.getByTestId('psp-approve').click()
  await expect(pspPage.getByTestId('psp-outcome')).toBeVisible()
  await expect(pspPage.getByText('Payment approved')).toBeVisible()

  // Multi-tab coordination: the original Error Lab tab is untouched and
  // still interactive while the PSP tab is open.
  await expect(page.getByTestId('psp-redirect-trigger')).toBeVisible()

  await pspPage.close()
  expect(context.pages()).toHaveLength(1)
})

test('declines in the PSP simulator tab', async ({ page, context }) => {
  await gotoErrorLab(page)

  const newPagePromise = context.waitForEvent('page')
  await page.getByTestId('psp-redirect-trigger').click()
  const pspPage = await newPagePromise
  // 'networkidle' (not the default 'load') so the click below lands after
  // Vue hydration has actually attached event listeners — this is a fresh
  // dev-server route compile, and SSR-rendered HTML is visible/"actionable"
  // to Playwright before hydration finishes wiring @click handlers.
  await pspPage.waitForLoadState('networkidle')

  await pspPage.getByTestId('psp-decline').click()
  await expect(pspPage.getByText('Payment declined')).toBeVisible()

  await pspPage.close()
})

test('PSP simulator is reachable without an authenticated session', async ({ browser }) => {
  // Fresh, unauthenticated context — no session mock at all. A real PSP
  // redirect target lives outside this app's session realm; this proves the
  // simulator page is not gated by auth.global.ts like every other route.
  const freshContext = await browser.newContext()
  const freshPage = await freshContext.newPage()

  await freshPage.goto('/psp-redirect-simulator')
  await expect(freshPage.getByTestId('psp-redirect-simulator')).toBeVisible({ timeout: 15000 })
  await expect(freshPage).toHaveURL(/\/psp-redirect-simulator$/)

  await freshContext.close()
})
