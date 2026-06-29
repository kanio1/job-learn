/**
 * F-D6: Console/PageError Monitoring + browser storage token leak guard.
 *
 * Pure helper functions — not fixtures, not POM.
 * Teach:
 *   - page.on('console')   — capture console.error messages
 *   - page.on('pageerror') — capture uncaught JS errors
 *   - page.evaluate()      — inspect localStorage/sessionStorage from the test
 *
 * Security invariant (sealed session architecture):
 *   The access token lives only in a server-sealed session cookie.
 *   It must never appear in localStorage, sessionStorage, or browser console output.
 */

import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'

/**
 * Asserts that neither localStorage nor sessionStorage contain a JWT or Bearer token.
 *
 * The BFF architecture stores the access token only in a server-side sealed session.
 * It must never reach browser storage APIs.
 *
 * Note: On assertion failure only a safe label is shown — raw storage contents are
 * intentionally NOT logged to prevent accidental secret exposure in CI reports.
 *
 * Playwright capability: page.evaluate() — run arbitrary JS in the page context.
 */
export async function expectNoTokenInBrowserStorage(page: Page): Promise<void> {
  const [localHasJwt, sessionHasJwt] = await page.evaluate(() => {
    const local = JSON.stringify(Object.entries(localStorage))
    const session = JSON.stringify(Object.entries(sessionStorage))
    return [
      local.includes('eyJ') || local.includes('Bearer '),
      session.includes('eyJ') || session.includes('Bearer '),
    ]
  })

  expect(
    localHasJwt,
    'localStorage must not contain a JWT (eyJ...) or Bearer token — sealed session architecture violated',
  ).toBe(false)
  expect(
    sessionHasJwt,
    'sessionStorage must not contain a JWT (eyJ...) or Bearer token — sealed session architecture violated',
  ).toBe(false)
}

/**
 * Attaches page.on('console') and page.on('pageerror') listeners.
 * Returns a getter for all collected error strings.
 *
 * Caller is responsible for filtering known framework noise before asserting.
 *
 * Playwright capabilities:
 *   - page.on('console')   — type === 'error' messages
 *   - page.on('pageerror') — uncaught JavaScript exceptions
 *
 * Usage:
 *   const getErrors = attachConsoleErrorGuard(page)
 *   // ... navigate and interact ...
 *   const unexpected = getErrors().filter(e => !e.includes('[nuxt]'))
 *   expect(unexpected).toHaveLength(0)
 */
export function attachConsoleErrorGuard(page: Page): () => string[] {
  const collected: string[] = []

  page.on('console', msg => {
    if (msg.type() === 'error') {
      collected.push(`console.error: ${msg.text()}`)
    }
  })

  page.on('pageerror', err => {
    collected.push(`pageerror: ${err.message}`)
  })

  return () => [...collected]
}
