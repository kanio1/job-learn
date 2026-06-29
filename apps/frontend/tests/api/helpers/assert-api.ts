/**
 * Minimal reusable assertion helpers for APIRequestContext tests (F-A1 / F-A3).
 *
 * These are pure functions — not fixtures, not POM.
 * Each function asserts one HTTP contract invariant and provides a descriptive
 * failure message so a failing test immediately names the broken contract.
 *
 * Usage:
 *   import { expectCorrelationIdHeader, expectProblemDetailsStructure } from './helpers/assert-api'
 */

import { expect } from '@playwright/test'

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const PROBLEM_TYPE_PREFIX = /^https:\/\/api\.payment-quality\.local\/problems\//

/**
 * Asserts that `x-correlation-id` header is present and is a valid UUID.
 * Returns the value so callers can compare it with the body's `correlationId` field.
 *
 * Playwright capability: response.headers() — capturing a named header value.
 */
export function expectCorrelationIdHeader(headers: Record<string, string>): string {
  const id = headers['x-correlation-id']
  expect(id, 'X-Correlation-ID header must be present').toBeTruthy()
  expect(id, 'X-Correlation-ID must be a valid UUID').toMatch(UUID_PATTERN)
  return id
}

/**
 * Asserts `content-type` contains `application/problem+json` (RFC 9457).
 *
 * Playwright capability: response.headers() — content negotiation assertion.
 */
export function expectProblemJsonContentType(headers: Record<string, string>): void {
  expect(
    headers['content-type'],
    'Problem responses must use Content-Type: application/problem+json',
  ).toContain('application/problem+json')
}

/**
 * Asserts `cache-control` contains `no-store`.
 * Payment error details must not be cached.
 *
 * Playwright capability: response.headers() — security header assertion.
 */
export function expectNoCacheStore(headers: Record<string, string>): void {
  expect(
    headers['cache-control'],
    'Error responses must have Cache-Control: no-store to prevent sensitive data caching',
  ).toContain('no-store')
}

/**
 * Asserts the required RFC 9457 Problem Details fields.
 * Validates: status (matches HTTP code), type (lab URI prefix), correlationId (UUID).
 *
 * Playwright capability: response.json() — structured body assertions.
 */
export function expectProblemDetailsStructure(
  body: Record<string, unknown>,
  expectedStatus: number,
): void {
  expect(
    body.status,
    `Problem Details 'status' must equal the HTTP response status ${expectedStatus}`,
  ).toBe(expectedStatus)
  expect(body.type, "Problem Details 'type' must be a string").toEqual(expect.any(String))
  expect(
    String(body.type),
    "Problem Details 'type' must use the lab problem URI prefix",
  ).toMatch(PROBLEM_TYPE_PREFIX)
  expect(
    body.correlationId,
    "Problem Details 'correlationId' must be a valid UUID",
  ).toMatch(UUID_PATTERN)
}

/**
 * Asserts that no Authorization bearer token is leaked in the response.
 * Checks both headers and raw body text.
 *
 * Security invariant: the BFF must never forward the backend JWT to the browser.
 *
 * Playwright capability: response.headers() + response.text() — security assertions.
 */
export function expectNoAuthTokenLeak(
  headers: Record<string, string>,
  rawBody: string,
): void {
  expect(
    headers['authorization'],
    'Authorization header must never appear in a response — token would be exposed to the browser',
  ).toBeUndefined()
  expect(
    rawBody,
    'Response body must not contain a raw JWT (Bearer eyJ...) — token leak detected',
  ).not.toContain('Bearer eyJ')
}
