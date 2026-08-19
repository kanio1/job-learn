/**
 * Property test: If-Match carries the latest ETag and updates on success.
 *
 * **Validates: Requirements 5.3, 5.6**
 *
 * Feature: payment-operations-dashboard, Property 11: If-Match carries the latest ETag and updates on success
 *
 * Two invariants are verified for every fast-check generated read→write sequence:
 *
 *  1. The `If-Match` value sent to the lifecycle API equals the ETag captured
 *     from the most recent Payment Order read (stored in `versionMarker`).
 *
 *  2. After a successful lifecycle write the store replaces `versionMarker`
 *     with the new ETag returned in the write response headers.
 *
 * Runs ≥ 100 iterations per property.
 */

// @vitest-environment nuxt
import { describe, it, expect, vi, beforeEach } from 'vitest'
import fc from 'fast-check'
import { setActivePinia, createPinia } from 'pinia'
import { usePaymentOrdersStore } from './payment-orders'
import type { ApiResponse } from '~/types/api'
import type { PaymentOrderResponse } from '~/schemas/payment-order.schema'

// ---------------------------------------------------------------------------
// Module-level mocks for composable dependencies
// ---------------------------------------------------------------------------

// Capture the If-Match value actually passed to each lifecycle call


const mockAuthorizeOrder = vi.fn()
const mockCaptureOrder = vi.fn()
const mockCancelOrder = vi.fn()
const mockRefundOrder = vi.fn()
const mockGetHistory = vi.fn()
const mockPatchMetadata = vi.fn()
const mockGetOrder = vi.fn()

vi.mock('~/composables/usePaymentLifecycleApi', () => ({
  usePaymentLifecycleApi: () => ({
    authorizeOrder: mockAuthorizeOrder,
    captureOrder: mockCaptureOrder,
    cancelOrder: mockCancelOrder,
    refundOrder: mockRefundOrder,
    getHistory: mockGetHistory,
    patchMetadata: mockPatchMetadata,
  }),
  mapStatusToCategory: (code: number) => {
    if (code === 412) return 'stale_state'
    if (code === 422) return 'invalid_transition'
    if (code === 409) return 'idempotency_conflict'
    if (code === 403) return 'forbidden'
    if (code === 404) return 'not_found'
    return 'validation'
  },
}))

vi.mock('~/composables/usePaymentOrdersApi', () => ({
  usePaymentOrdersApi: () => ({
    getOrder: mockGetOrder,
    listOrders: vi.fn(),
    getOrderSummary: vi.fn(),
    createOrder: vi.fn(),
  }),
}))

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Build a minimal PaymentOrderResponse for the mock. */
function makeOrderResponse(overrides: Partial<PaymentOrderResponse> = {}): PaymentOrderResponse {
  return {
    paymentOrderId: 'order-abc-123',
    merchantId: 'merchant-xyz-456',
    amountMinor: 1000,
    currency: 'PLN',
    clientOrderReference: 'REF-001',
    status: 'CREATED',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    versionMarker: null,
    ...overrides,
  }
}

/** Build a successful ApiResponse<PaymentOrderResponse> with the given ETag. */
function makeSuccessApiResponse(
  etag: string,
  status: PaymentOrderResponse['status'] = 'AUTHORIZED'
): ApiResponse<PaymentOrderResponse> {
  return {
    data: makeOrderResponse({ status }),
    status: 200,
    headers: { etag },
    problem: null,
    raw: '{}',
  }
}

/** Build an ApiResponse<PaymentOrderResponse> as returned by a GET (read). */
function makeReadApiResponse(etag: string): ApiResponse<PaymentOrderResponse> {
  return {
    data: makeOrderResponse({ versionMarker: null }),
    status: 200,
    headers: { etag },
    problem: null,
    raw: '{}',
  }
}

/** ETag arbitrary: opaque quoted string as per HTTP spec (e.g. `"abc"` or `"3"`). */
const etagArb = fc
  .string({ minLength: 1, maxLength: 40 })
  .filter((s) => !s.includes('"'))
  .map((s) => `"${s}"`)

/** Action arbitrary: one of the four lifecycle actions. */
const actionArb = fc.constantFrom<'authorize' | 'capture' | 'cancel' | 'refund'>(
  'authorize',
  'capture',
  'cancel',
  'refund'
)

// ---------------------------------------------------------------------------
// Test setup
// ---------------------------------------------------------------------------

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()

  // After a successful lifecycle action the store calls loadDetail + loadHistory.
  // Mock them to return minimal valid responses so the chain completes.
  mockGetHistory.mockResolvedValue({
    data: { content: [] },
    status: 200,
    headers: {},
    problem: null,
    raw: '{}',
  })
})

// ---------------------------------------------------------------------------
// Property 11: Invariant 1
// If-Match sent equals the latest captured ETag (stored in versionMarker)
// ---------------------------------------------------------------------------

describe(
  'Property 11 — Invariant 1: If-Match sent equals the latest captured ETag',
  () => {
    it(
      'for any read ETag, the lifecycle call receives that exact value as If-Match (≥100 iterations)',
      async () => {
        await fc.assert(
          fc.asyncProperty(etagArb, actionArb, async (readEtag, action) => {
            // Reset Pinia store for each iteration
            setActivePinia(createPinia())
            vi.clearAllMocks()

            const store = usePaymentOrdersStore()

            // Simulate the store capturing a read ETag via loadDetail.
            // getOrder returns the ETag in response.headers.etag.
            mockGetOrder.mockResolvedValue(makeReadApiResponse(readEtag))
            await store.loadDetail('merchant-1', 'order-1')

            expect(store.versionMarker).toBe(readEtag)

            // Prepare write mock to capture the If-Match value it receives.
            let capturedIfMatch: string | undefined
            const writeResponseEtag = `"${readEtag.replace(/"/g, '')}-v2"`

            const writeMock = vi.fn(
              async (
                _merchantId: string,
                _paymentOrderId: string,
                ifMatch: string,
                _idempotencyKey: string,
                _payload?: unknown
              ) => {
                capturedIfMatch = ifMatch
                return makeSuccessApiResponse(writeResponseEtag)
              }
            )

            // Also stub getOrder for the follow-up loadDetail call inside submitLifecycleAction
            mockGetOrder.mockResolvedValue(makeReadApiResponse(writeResponseEtag))
            mockGetHistory.mockResolvedValue({
              data: { content: [] },
              status: 200,
              headers: {},
              problem: null,
              raw: '{}',
            })

            // Wire the chosen action mock
            if (action === 'authorize') mockAuthorizeOrder.mockImplementation(writeMock)
            else if (action === 'capture') mockCaptureOrder.mockImplementation(writeMock)
            else if (action === 'cancel') mockCancelOrder.mockImplementation(writeMock)
            else mockRefundOrder.mockImplementation(writeMock)

            await store.submitLifecycleAction('merchant-1', 'order-1', action)

            // Invariant: If-Match forwarded to composable equals the versionMarker set by read
            expect(capturedIfMatch).toBe(readEtag)
          }),
          { numRuns: 100 }
        )
      }
    )
  }
)

// ---------------------------------------------------------------------------
// Property 11 — Invariant 2:
// On success, versionMarker is replaced with the new ETag from the write response
// ---------------------------------------------------------------------------

describe(
  'Property 11 — Invariant 2: successful write replaces versionMarker with response ETag',
  () => {
    it(
      'after a successful lifecycle action, versionMarker equals the ETag returned by the write (≥100 iterations)',
      async () => {
        await fc.assert(
          fc.asyncProperty(
            etagArb,
            etagArb.filter((e) => e !== '"initial"'),
            actionArb,
            async (initialEtag, newEtag, action) => {
              // Ensure the two ETags are different for this iteration to be meaningful
              fc.pre(initialEtag !== newEtag)

              setActivePinia(createPinia())
              vi.clearAllMocks()

              const store = usePaymentOrdersStore()

              // Step 1: read sets versionMarker from GET ETag
              mockGetOrder.mockResolvedValue(makeReadApiResponse(initialEtag))
              await store.loadDetail('merchant-1', 'order-1')
              expect(store.versionMarker).toBe(initialEtag)

              // Step 2: lifecycle write returns a NEW ETag in headers
              const writeResponse = makeSuccessApiResponse(newEtag)
              const writeMock = vi.fn().mockResolvedValue(writeResponse)

              // Stub follow-up calls inside submitLifecycleAction
              mockGetOrder.mockResolvedValue(makeReadApiResponse(newEtag))
              mockGetHistory.mockResolvedValue({
                data: { content: [] },
                status: 200,
                headers: {},
                problem: null,
                raw: '{}',
              })

              if (action === 'authorize') mockAuthorizeOrder.mockImplementation(writeMock)
              else if (action === 'capture') mockCaptureOrder.mockImplementation(writeMock)
              else if (action === 'cancel') mockCancelOrder.mockImplementation(writeMock)
              else mockRefundOrder.mockImplementation(writeMock)

              await store.submitLifecycleAction('merchant-1', 'order-1', action)

              // Invariant: versionMarker is now the new ETag from the write response
              expect(store.versionMarker).toBe(newEtag)
            }
          ),
          { numRuns: 100 }
        )
      }
    )
  }
)

// ---------------------------------------------------------------------------
// Property 11 — Invariant 3:
// Read→write→read sequence: versionMarker tracks the most recent ETag throughout
// ---------------------------------------------------------------------------

describe(
  'Property 11 — Invariant 3: versionMarker tracks the most recent ETag across a read→write→read sequence',
  () => {
    it(
      'after read(e1) → write(e2) → read(e3), versionMarker equals e3 (≥100 iterations)',
      async () => {
        // Generate three distinct ETags representing: initial read, write response, second read
        const distinctTripleArb = fc
          .tuple(etagArb, etagArb, etagArb)
          .filter(([a, b, c]) => a !== b && b !== c && a !== c)

        await fc.assert(
          fc.asyncProperty(distinctTripleArb, actionArb, async ([e1, e2, e3], action) => {
            setActivePinia(createPinia())
            vi.clearAllMocks()

            const store = usePaymentOrdersStore()

            // Read 1 → versionMarker = e1
            mockGetOrder.mockResolvedValue(makeReadApiResponse(e1))
            await store.loadDetail('merchant-1', 'order-1')
            expect(store.versionMarker).toBe(e1)

            // Write → response carries e2; follow-up loadDetail reads e2 from server
            const writeMock = vi.fn().mockResolvedValue(makeSuccessApiResponse(e2))
            mockGetOrder.mockResolvedValue(makeReadApiResponse(e2))
            mockGetHistory.mockResolvedValue({
              data: { content: [] },
              status: 200,
              headers: {},
              problem: null,
              raw: '{}',
            })

            if (action === 'authorize') mockAuthorizeOrder.mockImplementation(writeMock)
            else if (action === 'capture') mockCaptureOrder.mockImplementation(writeMock)
            else if (action === 'cancel') mockCancelOrder.mockImplementation(writeMock)
            else mockRefundOrder.mockImplementation(writeMock)

            await store.submitLifecycleAction('merchant-1', 'order-1', action)

            // After the write, the store did an internal loadDetail which set versionMarker = e2
            expect(store.versionMarker).toBe(e2)

            // Read 2 → versionMarker = e3
            mockGetOrder.mockResolvedValue(makeReadApiResponse(e3))
            await store.loadDetail('merchant-1', 'order-1')
            expect(store.versionMarker).toBe(e3)
          }),
          { numRuns: 100 }
        )
      }
    )
  }
)
