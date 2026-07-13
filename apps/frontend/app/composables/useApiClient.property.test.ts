/**
 * Property 10: Displayed status equals proxied backend status
 *
 * Validates: Requirements 6.3
 *
 * For any HTTP status code in the backend's response set, the `ApiResponse.status`
 * returned by `useApiClient` must equal the status code of the proxied response.
 * This ensures the UI never misrepresents the HTTP status it received.
 *
 * Tag: Feature: payment-operations-dashboard, Property 10: Displayed status equals proxied backend status
 */

// @vitest-environment nuxt
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import { useApiClient } from './useApiClient'
import { z } from 'zod'

// ─── Arbitraries ─────────────────────────────────────────────────────────────

/**
 * The complete set of HTTP status codes the backend uses per the problem
 * contract and the Current Backend Endpoint Audit.
 */
const backendStatusCodes = [200, 201, 400, 401, 403, 404, 406, 409, 412, 415, 422, 428, 500] as const
type BackendStatusCode = typeof backendStatusCodes[number]

const backendStatusArb: fc.Arbitrary<BackendStatusCode> = fc.constantFrom(...backendStatusCodes)

/** Simple pass-through schema used when the test needs schema validation to succeed. */
const anySchema = z.unknown()

// ─── Mock helpers ─────────────────────────────────────────────────────────────

/**
 * Build a Headers-like object with a minimal get() method.
 */
function makeHeaders(contentType = 'application/json'): Headers {
  const map = new Map<string, string>([['content-type', contentType]])
  return {
    get: (k: string) => map.get(k.toLowerCase()) ?? null,
  } as unknown as Headers
}

/**
 * Simulate a successful $fetch.raw response (2xx).
 */
function makeSuccessResponse(status: number, data: unknown = { ok: true }) {
  return {
    status,
    headers: makeHeaders('application/json'),
    _data: data,
  }
}

/**
 * Simulate a $fetch.raw error throw (non-2xx).
 * ofetch / $fetch.raw throws an FetchError with `response` attached for non-2xx.
 */
function makeErrorThrow(status: number, data: unknown = null, contentType = 'application/json') {
  const headers = makeHeaders(contentType)
  const error = Object.assign(new Error('HTTP error'), {
    statusCode: status,
    statusMessage: `HTTP ${status}`,
    response: {
      status,
      headers,
      _data: data,
    },
    data,
  })
  return error
}

// ─── Tests ─────────────────────────────────────────────────────────────────────

describe('Property 10: Displayed status equals proxied backend status', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  it('2xx — ApiResponse.status equals the response status for success codes', async () => {
    const successCodes = backendStatusCodes.filter(c => c >= 200 && c < 300) // [200, 201]

    await fc.assert(
      fc.asyncProperty(
        fc.constantFrom(...successCodes),
        async (status) => {
          // Arrange: mock $fetch.raw to return a successful response
          vi.stubGlobal('$fetch', Object.assign(
            () => {},
            {
              raw: vi.fn().mockResolvedValue(makeSuccessResponse(status, { ok: true })),
            },
          ))

          const { request } = useApiClient()

          // Act
          const result = await request('/server/api/test', anySchema)

          // Assert: the status in the envelope exactly mirrors the proxied status
          expect(result.status).toBe(status)
        },
      ),
      { numRuns: 100 },
    )
  })

  it('4xx/5xx — ApiResponse.status equals the error status for error codes', async () => {
    const errorCodes = backendStatusCodes.filter(c => c >= 400) // all error codes

    await fc.assert(
      fc.asyncProperty(
        fc.constantFrom(...errorCodes),
        async (status) => {
          // Arrange: mock $fetch.raw to throw (as ofetch does for non-2xx)
          const problemBody = {
            type: 'about:blank',
            title: `HTTP ${status}`,
            status,
            detail: `Triggered ${status}`,
          }
          vi.stubGlobal('$fetch', Object.assign(
            () => {},
            {
              raw: vi.fn().mockRejectedValue(
                makeErrorThrow(status, problemBody, 'application/problem+json'),
              ),
            },
          ))

          const { request } = useApiClient()

          // Act
          const result = await request('/server/api/test', anySchema)

          // Assert: captured status == the thrown error's status code
          expect(result.status).toBe(status)
        },
      ),
      { numRuns: 100 },
    )
  })

  it('preserves a duplicate merchant Problem Details detail for the UI', async () => {
    const duplicateProblem = {
      type: 'https://api.payment-quality.local/problems/duplicate-merchant-reference',
      title: 'Merchant already exists',
      status: 409,
      detail: 'A merchant with this reference already exists',
      error: 'duplicate_merchant_reference',
    }
    vi.stubGlobal('$fetch', Object.assign(
      () => {},
      {
        raw: vi.fn().mockRejectedValue(
          makeErrorThrow(409, duplicateProblem, 'application/problem+json'),
        ),
      },
    ))

    const { request } = useApiClient()
    const result = await request('/api/merchants', anySchema, { method: 'POST' })

    expect(result.status).toBe(409)
    expect(result.data).toBeNull()
    expect(result.problem).toMatchObject(duplicateProblem)
    expect(result.problem?.detail).toBe('A merchant with this reference already exists')
  })

  it('full status range — ApiResponse.status equals proxied status across all backend codes', async () => {
    await fc.assert(
      fc.asyncProperty(
        backendStatusArb,
        async (status) => {
          const is2xx = status >= 200 && status < 300

          if (is2xx) {
            vi.stubGlobal('$fetch', Object.assign(
              () => {},
              {
                raw: vi.fn().mockResolvedValue(makeSuccessResponse(status, { value: status })),
              },
            ))
          } else {
            vi.stubGlobal('$fetch', Object.assign(
              () => {},
              {
                raw: vi.fn().mockRejectedValue(
                  makeErrorThrow(status, {
                    type: 'about:blank',
                    title: `HTTP ${status}`,
                    status,
                    detail: `Error ${status}`,
                  }, 'application/problem+json'),
                ),
              },
            ))
          }

          const { request } = useApiClient()
          const result = await request('/server/api/test', anySchema)

          // Core property: status in the envelope == status from the proxy
          expect(result.status).toBe(status)
        },
      ),
      { numRuns: 100 },
    )
  })
})

// ─────────────────────────────────────────────────────────────────────────────
// Property 4: Inbound response validation gating
//
// Validates: Requirements 10.4, 10.5
//
// Properties under test:
//   - When a valid payload passes the Zod schema → data is non-null, problem null
//   - When an invalid payload fails the Zod schema → data is null, problem set,
//     no unvalidated data exposed via `data`
//
// Tag: Feature: payment-operations-dashboard, Property 4: Inbound response validation gating
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Representative domain schema used for Property 4 — mirrors the shape of
 * objects the dashboard renders (status enums, numeric amounts, uuid ids).
 */
const p4ItemSchema = z.object({
  id: z.string().uuid(),
  amount: z.number().int().positive(),
  status: z.enum(['CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'REFUNDED']),
})

type P4Item = z.infer<typeof p4ItemSchema>

// ─── Arbitraries for Property 4 ───────────────────────────────────────────────

/** Valid items that will pass p4ItemSchema */
const validP4ItemArb: fc.Arbitrary<P4Item> = fc.record({
  id: fc.uuid(),
  amount: fc.integer({ min: 1, max: 100_000_000 }),
  status: fc.constantFrom('CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'REFUNDED') as fc.Arbitrary<P4Item['status']>,
})

/** Invalid payloads — at least one structural violation of p4ItemSchema */
const invalidP4PayloadArb: fc.Arbitrary<unknown> = fc.oneof(
  // missing `status` field
  fc.record({ id: fc.uuid(), amount: fc.integer({ min: 1, max: 1000 }) }),
  // `amount` is a string (wrong type)
  fc.record({ id: fc.uuid(), amount: fc.string(), status: fc.constant('CREATED') }),
  // invalid status value not in the enum
  fc.record({
    id: fc.uuid(),
    amount: fc.integer({ min: 1, max: 1000 }),
    status: fc.string().filter(s => !['CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'REFUNDED'].includes(s)),
  }),
  // amount = 0 (fails positive() constraint)
  fc.record({ id: fc.uuid(), amount: fc.constant(0), status: fc.constant('CREATED') }),
  // null body
  fc.constant(null),
  // completely empty object
  fc.constant({}),
)

/** 2xx status codes (schema validation applies to success responses) */
const p4SuccessStatusArb = fc.integer({ min: 200, max: 299 })

describe('Feature: payment-operations-dashboard, Property 4: Inbound response validation gating', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  it('Property 4a — valid payloads: data is returned, problem is null (≥100 iterations)', async () => {
    // **Validates: Requirements 10.4, 10.5**
    await fc.assert(
      fc.asyncProperty(validP4ItemArb, p4SuccessStatusArb, async (validPayload, status) => {
        vi.stubGlobal('$fetch', Object.assign(
          () => {},
          { raw: vi.fn().mockResolvedValue(makeSuccessResponse(status, validPayload)) },
        ))

        const { request } = useApiClient()
        const result = await request('/server/api/test', p4ItemSchema)

        // Req 10.4 — every response validated before rendering
        // Req 10.5 — valid payload must result in non-null data
        expect(result.data).not.toBeNull()
        expect(result.data).toMatchObject(validPayload)
        expect(result.problem).toBeNull()
        expect(result.status).toBe(status)
      }),
      { numRuns: 100, verbose: false },
    )
  })

  it('Property 4b — invalid payloads: data is null, problem describes failure (≥100 iterations)', async () => {
    // **Validates: Requirements 10.4, 10.5**
    await fc.assert(
      fc.asyncProperty(invalidP4PayloadArb, p4SuccessStatusArb, async (invalidPayload, status) => {
        vi.stubGlobal('$fetch', Object.assign(
          () => {},
          { raw: vi.fn().mockResolvedValue(makeSuccessResponse(status, invalidPayload)) },
        ))

        const { request } = useApiClient()
        const result = await request('/server/api/test', p4ItemSchema)

        // Req 10.5 — MUST NOT render any unvalidated data
        expect(result.data).toBeNull()

        // An error must be surfaced so the UI can show ErrorState
        expect(result.problem).not.toBeNull()
        expect(result.problem?.title).toBe('Response Validation Error')

        // Status is forwarded even on validation failure (UI needs the code)
        expect(result.status).toBe(status)
      }),
      { numRuns: 100, verbose: false },
    )
  })

  it('Property 4c — invalid payloads: raw body is always a string even when data is null (≥100 iterations)', async () => {
    // raw must always be capturable for RawJsonViewer (Req 4.3, 8.6)
    await fc.assert(
      fc.asyncProperty(invalidP4PayloadArb, p4SuccessStatusArb, async (invalidPayload, status) => {
        vi.stubGlobal('$fetch', Object.assign(
          () => {},
          { raw: vi.fn().mockResolvedValue(makeSuccessResponse(status, invalidPayload)) },
        ))

        const { request } = useApiClient()
        const result = await request('/server/api/test', p4ItemSchema)

        expect(typeof result.raw).toBe('string')
      }),
      { numRuns: 100, verbose: false },
    )
  })
})
