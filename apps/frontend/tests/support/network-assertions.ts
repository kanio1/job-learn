/**
 * F-A3: Network Header Assertion Patterns — helpers for page.waitForResponse() results.
 *
 * Pure helper functions that accept a structural response type matching both
 * browser-network Response (page.waitForResponse) and APIResponse (request.*).
 * This avoids import conflicts with the global Response type.
 *
 * Teach:
 *   - response.status()  — HTTP status code from the intercepted response
 *   - response.headers() — lowercase header map from the intercepted response
 */

import { expect } from '@playwright/test'

/** Minimal structural type that matches both browser Response and APIResponse. */
type HasHeaders = {
  headers(): Record<string, string>
}

/**
 * Asserts that Retry-After is present and is a positive integer.
 * Returns the parsed seconds value for further comparison.
 *
 * Playwright capability: response.headers() via page.waitForResponse()
 */
export function expectRetryAfterHeader(response: HasHeaders): number {
  const raw = response.headers()['retry-after']
  expect(raw, 'Retry-After header must be present in the network response').toBeTruthy()
  const seconds = parseInt(raw, 10)
  expect(
    Number.isInteger(seconds),
    `Retry-After header value '${raw}' must parse to an integer`,
  ).toBe(true)
  expect(seconds, 'Retry-After value must be greater than 0').toBeGreaterThan(0)
  return seconds
}

/**
 * Asserts that the Authorization header does NOT appear in the response.
 * The BFF must never forward the backend JWT to the browser layer.
 *
 * Playwright capability: response.headers() via page.waitForResponse()
 */
export function expectNoAuthorizationInNetworkResponse(response: HasHeaders): void {
  expect(
    response.headers()['authorization'],
    'Authorization header must never appear in a response — BFF token leak detected',
  ).toBeUndefined()
}
