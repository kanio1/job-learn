/**
 * F-A1: APIRequestContext Foundation — Error Lab API Contract Tests
 *
 * These tests call the BFF Error Lab endpoints using APIRequestContext only.
 * No browser context, no UI, no Keycloak session required for the standalone triggers.
 *
 * Playwright capabilities demonstrated (Phase 3A-1):
 *   - request fixture (APIRequestContext) — API calls without a browser
 *   - response.status()     — asserting the HTTP status code
 *   - response.ok()         — checking response.ok() === false for error responses
 *   - response.headers()    — capturing response headers (lowercase keys)
 *   - response.json()       — parsing a JSON body from an API response
 *   - response.text()       — reading the raw response body as string
 *   - expect(body).toMatchObject() — partial object matching for Problem Details
 *
 * Run requirements:
 *   - Nuxt dev server must be running (started automatically by webServer in playwright.config.ts)
 *   - No real backend required for trigger-429 (standalone BFF mock)
 *   - No Keycloak required for trigger-429
 *
 * Full-stack scenarios (trigger-401, trigger-428, idempotency-replay, trigger-304)
 * require a running Spring backend and authenticated Keycloak session.
 * Those are scoped to Phase 3A-2 / Phase 3A-4 after multi-role auth is ready.
 */

import { expect, test } from '@playwright/test'
import {
  expectCorrelationIdHeader,
  expectNoCacheStore,
  expectNoAuthTokenLeak,
  expectProblemDetailsStructure,
  expectProblemJsonContentType,
} from './helpers/assert-api'

const TRIGGER_429 = '/api/error-lab/trigger-429'

// ─── 429 Too Many Requests — target scenario 1 + 6 + 7 ────────────────────────

test.describe('Error Lab API — 429 Too Many Requests (F-A1)', () => {
  /**
   * Target: scenario 1 — 429 Retry-After + retryable Problem Details
   *
   * Capability: response.status()
   * The most fundamental APIRequestContext assertion: checking the HTTP status code.
   * No headers, no body parsing — just the code.
   */
  test('returns HTTP 429 status code', async ({ request }) => {
    const response = await request.post(TRIGGER_429)

    expect(response.status()).toBe(429)
  })

  /**
   * Capability: response.ok()
   * response.ok() returns true only for 200–299. For 429 it must be false.
   * Useful as a guard in higher-level helpers before parsing the body.
   */
  test('response.ok() is false for a 429 error response', async ({ request }) => {
    const response = await request.post(TRIGGER_429)

    expect(response.ok()).toBe(false)
  })

  /**
   * Target: scenario 7 — retryAfterSeconds matches Retry-After
   *
   * Capability: response.headers() — Retry-After
   * Asserts the numeric value of the Retry-After header.
   * Note: response.headers() returns all header names in lowercase.
   */
  test('Retry-After header is present and is a positive integer', async ({ request }) => {
    const response = await request.post(TRIGGER_429)
    const headers = response.headers()

    expect(headers['retry-after'], 'Retry-After header must be present').toBeTruthy()
    const retryAfterSeconds = parseInt(headers['retry-after'], 10)
    expect(Number.isInteger(retryAfterSeconds), 'Retry-After must parse to an integer').toBe(true)
    expect(retryAfterSeconds, 'Retry-After must be > 0').toBeGreaterThan(0)
  })

  /**
   * Capability: response.headers() — X-Correlation-ID
   * Every backend/BFF response must carry a UUID correlation ID for distributed tracing.
   */
  test('X-Correlation-ID response header is a valid UUID', async ({ request }) => {
    const response = await request.post(TRIGGER_429)

    expectCorrelationIdHeader(response.headers())
  })

  /**
   * Capability: response.headers() — Content-Type
   * RFC 9457 Problem Details responses must use application/problem+json.
   */
  test('Content-Type is application/problem+json (RFC 9457)', async ({ request }) => {
    const response = await request.post(TRIGGER_429)

    expectProblemJsonContentType(response.headers())
  })

  /**
   * Capability: response.headers() — Cache-Control
   * Error details must not be cached in browser or intermediary caches.
   */
  test('Cache-Control is no-store (sensitive error data must not be cached)', async ({ request }) => {
    const response = await request.post(TRIGGER_429)

    expectNoCacheStore(response.headers())
  })

  /**
   * Capability: response.json()
   * Parses the response body as JSON and validates the RFC 9457 Problem Details structure.
   * Fields: status, type, correlationId.
   */
  test('response body is a valid RFC 9457 Problem Details object', async ({ request }) => {
    const response = await request.post(TRIGGER_429)
    const body = await response.json()

    expectProblemDetailsStructure(body, 429)
  })

  /**
   * Target: scenario 1 (extended) — retryable Problem Details extension fields
   *
   * Capability: response.json() — domain-specific extension fields
   * `retryable` and `retryAfterSeconds` are non-standard extensions the lab adds to
   * Problem Details. These are the fields a client would use to implement auto-retry.
   */
  test('Problem Details body has retryable:true and retryAfterSeconds > 0', async ({ request }) => {
    const response = await request.post(TRIGGER_429)
    const body = await response.json()

    expect(body.retryable, "retryable extension field must be true for 429 'rate_limit_exceeded'").toBe(true)
    expect(typeof body.retryAfterSeconds, 'retryAfterSeconds must be a number').toBe('number')
    expect(body.retryAfterSeconds, 'retryAfterSeconds must be positive').toBeGreaterThan(0)
  })

  /**
   * Target: scenario 7 — retryAfterSeconds matches Retry-After
   *
   * Capability: response.headers() + response.json() — cross-layer contract
   * The HTTP Retry-After header and the Problem Details retryAfterSeconds field
   * must agree. A client must not need to read both; this test verifies they do not diverge.
   */
  test('retryAfterSeconds in body matches Retry-After header value', async ({ request }) => {
    const response = await request.post(TRIGGER_429)
    const headers = response.headers()
    const body = await response.json()

    const headerSeconds = parseInt(headers['retry-after'], 10)
    expect(
      body.retryAfterSeconds,
      'retryAfterSeconds in Problem Details body must equal the Retry-After header',
    ).toBe(headerSeconds)
  })

  /**
   * Capability: response.headers() + response.json() — correlation layer contract
   * X-Correlation-ID header and the `correlationId` field in the Problem Details body
   * must carry the same value. Both the network layer and the application body must agree.
   */
  test('X-Correlation-ID header matches correlationId in Problem Details body', async ({ request }) => {
    const response = await request.post(TRIGGER_429)
    const correlationId = expectCorrelationIdHeader(response.headers())
    const body = await response.json()

    expect(
      body.correlationId,
      "Problem Details 'correlationId' field must match X-Correlation-ID response header",
    ).toBe(correlationId)
  })

  /**
   * Target: scenario 6 — no Authorization leakage
   *
   * Capability: response.text() + response.headers() — security assertions
   * Critical BFF security invariant: the backend JWT must never appear in the response
   * sent to the browser. Applies to both response headers and body text.
   */
  test('no Authorization token is leaked in response headers or body', async ({ request }) => {
    const response = await request.post(TRIGGER_429)
    const rawBody = await response.text()

    expectNoAuthTokenLeak(response.headers(), rawBody)
  })

  /**
   * Capability: multiple independent requests — per-request uniqueness
   * Correlation IDs must never be reused across requests. This ensures traces
   * in distributed systems can be uniquely identified.
   */
  test('each request generates a distinct X-Correlation-ID', async ({ request }) => {
    const [r1, r2] = await Promise.all([
      request.post(TRIGGER_429),
      request.post(TRIGGER_429),
    ])

    const id1 = r1.headers()['x-correlation-id']
    const id2 = r2.headers()['x-correlation-id']
    expect(id1, 'First X-Correlation-ID must be truthy').toBeTruthy()
    expect(id2, 'Second X-Correlation-ID must be truthy').toBeTruthy()
    expect(id1, 'X-Correlation-ID must be unique per request (not a fixed/replayed value)').not.toBe(id2)
  })

  /**
   * Capability: expect(body).toMatchObject() — structured partial matching
   * Tests the complete Problem Details contract in a single readable assertion.
   * toMatchObject is the preferred pattern for asserting response body shape
   * because it ignores extra fields (title, detail, etc.) while catching regressions.
   */
  test('complete Problem Details contract asserted with toMatchObject', async ({ request }) => {
    const response = await request.post(TRIGGER_429)
    expect(response.status()).toBe(429)
    const body = await response.json()

    expect(body).toMatchObject({
      status: 429,
      type: expect.stringContaining('/problems/'),
      retryable: true,
      retryAfterSeconds: expect.any(Number),
      correlationId: expect.stringMatching(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
      ),
    })
  })
})

// ─── Full-stack scenarios — require Spring backend + Keycloak session ──────────
//
// The following tests are DEFERRED to Phase 3A-2/3A-4.
// They need:
//   - Spring Boot backend running (port 8080)
//   - Authenticated Nuxt session (real Keycloak or mocked session cookie)
//
// trigger-401: needs backend (calls /api/merchants without token → backend returns 401)
// trigger-428: needs auth session + backend (multi-step: find merchant, create order, authorize without If-Match)
// trigger-idempotency-replay: needs auth session + backend (two POST calls with same Idempotency-Key)
// trigger-304: needs auth session + backend (GET → capture ETag → conditional GET → 304)
//
// These will be enabled once Phase 3A-4 (multi-role auth + worker isolation) is complete.
