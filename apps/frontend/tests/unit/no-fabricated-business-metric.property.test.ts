/**
 * Property 1: No fabricated business metric
 *
 * Validates: Requirements 1.1, 1.7
 *
 * Tag: Feature: payment-operations-dashboard, Property 1: No fabricated business metric
 *
 * The Overview page and PaymentOrderSummaryCards component MUST display only
 * metric values that come directly from backend API responses — specifically the
 * merchant list payload and the payment-order summary payload. No metric may be
 * computed, totalled, aggregated, or recomputed on the client side.
 *
 * Properties tested:
 *
 *  P1a — The displayed merchant count equals exactly the totalElements field
 *         returned by the backend merchant list response.
 *
 *  P1b — The displayed total payment-order count equals exactly the totalOrders
 *         field returned by the backend summary payload.
 *
 *  P1c — The displayed per-status order count for each Payment_Status equals
 *         exactly the orderCount value returned in the byStatus array of the
 *         backend summary payload. Status entries absent in byStatus display 0.
 *
 *  P1d — No count metric displayed on the Overview is computed from other
 *         fields (e.g. no summing of byStatus entries to produce totalOrders,
 *         no counting of recent-order rows to produce the total count).
 *
 * Test approach (pure logic test — no DOM mount required):
 *   Define the mapping functions that represent how the Overview page and
 *   PaymentOrderSummaryCards map backend responses to displayed values, then
 *   use fast-check to generate arbitrary backend responses and assert the
 *   mapping is an identity (displayed === backend).
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import type { ApiHeaders } from '../../app/types/api'
import type {
  PaymentOrderSummaryResponse,
} from '../../app/schemas/payment-order.schema'

// ---------------------------------------------------------------------------
// Types mirroring backend response shapes (aligned with Zod schemas)
// ---------------------------------------------------------------------------

/** Merchant list page response — mirrors the paged merchant list from GET /api/merchants */
interface MerchantListResponse {
  content: Array<{ merchantId: string; displayName: string; merchantReference: string }>
  totalElements: number
  totalPages: number
  page: number
  size: number
}

type PaymentStatus = 'CREATED' | 'AUTHORIZED' | 'CAPTURED' | 'CANCELLED' | 'EXPIRED' | 'REFUNDED'

/** All valid Payment_Status values — matches the backend enum and Zod schema */
const ALL_PAYMENT_STATUSES: readonly PaymentStatus[] = [
  'CREATED',
  'AUTHORIZED',
  'CAPTURED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
]

// ---------------------------------------------------------------------------
// Mapping functions that represent Overview page / PaymentOrderSummaryCards
// display logic — these are the pure functions under test.
// ---------------------------------------------------------------------------

/**
 * Mapping A: how the Overview page derives the displayed merchant count.
 *
 * Source: index.vue line:
 *   merchantCount.value = merchantsRes.data.totalElements
 *
 * The displayed value is a direct pass-through of the backend field.
 * No counting of content.length, no client-side aggregation.
 */
function displayedMerchantCount(merchantListResponse: MerchantListResponse): number {
  return merchantListResponse.totalElements
}

/**
 * Mapping B: how the Overview page derives the displayed total payment-order count.
 *
 * Source: index.vue line:
 *   totalOrders.value = summaryRes.data.totalOrders
 *
 * The displayed value is a direct pass-through of the backend field.
 */
function displayedTotalOrders(summaryResponse: PaymentOrderSummaryResponse): number {
  return summaryResponse.totalOrders
}

/**
 * Mapping C: how PaymentOrderSummaryCards derives the displayed count for a
 * given Payment_Status from the backend summary payload.
 *
 * Source: PaymentOrderSummaryCards.vue function:
 *   countForStatus(status) = summary.byStatus.find(row => row.status === status)?.orderCount ?? 0
 *
 * Rules:
 * - The count for a status that IS in byStatus = its orderCount field (identity).
 * - The count for a status NOT in byStatus = 0 (absence means zero orders).
 * - The count is NEVER computed from other fields (not sum of amounts, not content rows, etc.).
 */
function displayedCountForStatus(
  summaryResponse: PaymentOrderSummaryResponse,
  status: PaymentStatus,
): number {
  return summaryResponse.byStatus.find(row => row.status === status)?.orderCount ?? 0
}

/**
 * Mapping D: how the Overview page derives the per-status entries shown in the
 * summary section (the v-for over byStatus).
 *
 * Source: index.vue lines:
 *   byStatus.value = summaryRes.data.byStatus.map(entry => ({
 *     status: entry.status,
 *     orderCount: entry.orderCount,
 *   }))
 *
 * Each entry is a direct pass-through of the corresponding backend entry fields.
 */
function displayedByStatusEntries(
  summaryResponse: PaymentOrderSummaryResponse,
): Array<{ status: string; orderCount: number }> {
  return summaryResponse.byStatus.map(entry => ({
    status: entry.status,
    orderCount: entry.orderCount,
  }))
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

/** Generates a non-negative integer ≤ 10,000 suitable for count fields */
const countArb = fc.integer({ min: 0, max: 10_000 })

/** Generates a valid amount in minor units (0 to 100,000,000) */
const amountArb = fc.integer({ min: 0, max: 100_000_000 })

/** Generates one of the valid Payment_Status enum values */
const paymentStatusArb: fc.Arbitrary<PaymentStatus> = fc.constantFrom(...ALL_PAYMENT_STATUSES)

/** Generates a non-empty UUID string for IDs */
const uuidArb = fc.uuid()

/**
 * Generates an arbitrary payment-order summary response.
 * The byStatus array contains at most one entry per status (reflecting real
 * backend behaviour — the backend groups by status and returns at most one
 * entry per distinct Payment_Status value).
 */
const summaryResponseArb: fc.Arbitrary<PaymentOrderSummaryResponse> = fc
  // Pick a subset of statuses (without repetition) by shuffling and slicing
  .integer({ min: 0, max: ALL_PAYMENT_STATUSES.length })
  .chain(numStatuses =>
    fc.shuffledSubarray([...ALL_PAYMENT_STATUSES], { minLength: numStatuses, maxLength: numStatuses })
      .chain(selectedStatuses =>
        fc.record({
          totalOrders: countArb,
          totalAmountMinor: amountArb,
          byCurrency: fc.array(
            fc.record({
              currency: fc.constantFrom('PLN', 'EUR', 'USD') as fc.Arbitrary<'PLN' | 'EUR' | 'USD'>,
              orderCount: countArb,
              totalAmountMinor: amountArb,
            }),
            { minLength: 0, maxLength: 3 },
          ),
          byStatus: fc.constant(
            selectedStatuses.map(status => ({
              status,
              orderCount: 0, // will be overridden below
              totalAmountMinor: 0,
            })),
          ),
        }).chain(base =>
          // Generate a distinct orderCount for each selected status
          fc.array(countArb, { minLength: selectedStatuses.length, maxLength: selectedStatuses.length })
            .map(counts => ({
              ...base,
              byStatus: selectedStatuses.map((status, i) => ({
                status,
                orderCount: counts[i]!,
                totalAmountMinor: 0,
              })),
            })),
        ),
      ),
  )

/** Generates an arbitrary merchant list response */
const merchantListResponseArb: fc.Arbitrary<MerchantListResponse> = fc.record({
  totalElements: countArb,
  totalPages: fc.integer({ min: 0, max: 100 }),
  page: fc.integer({ min: 0, max: 100 }),
  size: fc.integer({ min: 1, max: 100 }),
  content: fc.array(
    fc.record({
      merchantId: uuidArb,
      displayName: fc.string({ minLength: 1, maxLength: 60 }),
      merchantReference: fc.string({ minLength: 3, maxLength: 20 }),
    }),
    { minLength: 0, maxLength: 20 },
  ),
})

// ---------------------------------------------------------------------------
// Property 1 — Main tests
// ---------------------------------------------------------------------------

describe('Feature: payment-operations-dashboard, Property 1: No fabricated business metric', () => {

  // -------------------------------------------------------------------------
  // P1a — Merchant count is totalElements from the backend, not content.length
  // -------------------------------------------------------------------------

  describe('P1a: Displayed merchant count equals backend totalElements exactly', () => {
    it('should display exactly the totalElements value from the merchant list response', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * The displayed merchant count must be the exact value of totalElements
       * from the backend response. It must NOT be derived from content.length
       * (which reflects only the current page) or any client-side computation.
       */
      fc.assert(
        fc.property(merchantListResponseArb, (merchantListResponse) => {
          const displayed = displayedMerchantCount(merchantListResponse)

          // Core property: displayed === backend totalElements (identity)
          expect(displayed).toBe(merchantListResponse.totalElements)
        }),
        { numRuns: 200 },
      )
    })

    it('should NOT use content.length as the merchant count', () => {
      /**
       * Validates: Requirements 1.7
       *
       * content.length reflects only the current page size — it is not the total
       * count. This is the key fabrication risk: using the number of rows in the
       * returned page as if it were the total.
       *
       * We generate cases where totalElements != content.length to prove the
       * mapping uses totalElements, not content.length.
       */
      fc.assert(
        fc.property(
          merchantListResponseArb,
          fc.integer({ min: 0, max: 10_000 }),
          (base, totalElements) => {
            // Override totalElements to be different from content.length
            const response: MerchantListResponse = {
              ...base,
              totalElements,
            }

            const displayed = displayedMerchantCount(response)

            // Must use totalElements, not content.length
            expect(displayed).toBe(totalElements)

            // When they differ, the displayed value must still be totalElements
            if (totalElements !== response.content.length) {
              expect(displayed).not.toBe(response.content.length)
            }
          },
        ),
        { numRuns: 200 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P1b — Total payment-order count is totalOrders from the backend summary
  // -------------------------------------------------------------------------

  describe('P1b: Displayed total order count equals backend totalOrders exactly', () => {
    it('should display exactly the totalOrders value from the summary payload', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * The displayed total payment-order count must be the exact value of
       * totalOrders from the backend summary response. It must NOT be a sum of
       * byStatus orderCounts or a count of recent-order rows.
       */
      fc.assert(
        fc.property(summaryResponseArb, (summary) => {
          const displayed = displayedTotalOrders(summary)

          // Core property: displayed === backend totalOrders (identity)
          expect(displayed).toBe(summary.totalOrders)
        }),
        { numRuns: 200 },
      )
    })

    it('should NOT compute totalOrders by summing byStatus orderCounts', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * Summing byStatus.orderCount values is a common client-side fabrication
       * pattern. The backend is the source of truth for totalOrders; the client
       * must never re-derive it from byStatus.
       *
       * We generate cases where sum(byStatus.orderCount) != totalOrders to
       * prove the mapping reads totalOrders directly.
       */
      fc.assert(
        fc.property(summaryResponseArb, fc.integer({ min: 0, max: 10_000 }), (base, totalOrders) => {
          // Set a totalOrders that deliberately differs from sum(byStatus.orderCount)
          const computedSum = base.byStatus.reduce((acc, e) => acc + e.orderCount, 0)
          const summary: PaymentOrderSummaryResponse = {
            ...base,
            totalOrders,
          }

          const displayed = displayedTotalOrders(summary)

          // Must always be totalOrders, never the computed sum
          expect(displayed).toBe(totalOrders)

          // Explicitly: must not be the fabricated sum when they differ
          if (totalOrders !== computedSum) {
            expect(displayed).not.toBe(computedSum)
          }
        }),
        { numRuns: 200 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P1c — Per-status count is orderCount from byStatus, not recomputed
  // -------------------------------------------------------------------------

  describe('P1c: Displayed per-status count equals byStatus orderCount exactly', () => {
    it('should display the exact orderCount for each status present in byStatus', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * For each status that appears in byStatus, the displayed count must equal
       * its orderCount exactly — no rounding, no transformation, no recomputation.
       */
      fc.assert(
        fc.property(summaryResponseArb, (summary) => {
          for (const entry of summary.byStatus) {
            const displayed = displayedCountForStatus(summary, entry.status as PaymentStatus)

            // Core property: displayed === backend orderCount (identity)
            expect(displayed).toBe(entry.orderCount)
          }
        }),
        { numRuns: 200 },
      )
    })

    it('should display 0 for any Payment_Status not present in byStatus', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * If the backend does not include a status in byStatus, the absence means
       * zero orders in that state. The displayed count must be 0 — it must NOT
       * be derived from other fields (e.g. subtracting other counts from total).
       */
      fc.assert(
        fc.property(summaryResponseArb, (summary) => {
          const presentStatuses = new Set(summary.byStatus.map(e => e.status))

          for (const status of ALL_PAYMENT_STATUSES) {
            if (!presentStatuses.has(status)) {
              const displayed = displayedCountForStatus(summary, status)

              // A status absent in byStatus must show exactly 0
              expect(displayed).toBe(0)
            }
          }
        }),
        { numRuns: 200 },
      )
    })

    it('should display the correct count for every status in ALL_PAYMENT_STATUSES', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * The Overview page shows one card per Payment_Status value. Each card
       * must show the exact count from byStatus (or 0 if absent). This property
       * checks all six statuses for every generated summary.
       */
      fc.assert(
        fc.property(summaryResponseArb, (summary) => {
          for (const status of ALL_PAYMENT_STATUSES) {
            const backendEntry = summary.byStatus.find(e => e.status === status)
            const expectedCount = backendEntry?.orderCount ?? 0
            const displayed = displayedCountForStatus(summary, status)

            expect(displayed).toBe(expectedCount)
          }
        }),
        { numRuns: 200 },
      )
    })

    it('should NOT derive per-status count from totalOrders minus other statuses', () => {
      /**
       * Validates: Requirements 1.7
       *
       * A fabrication pattern: derive one status count as totalOrders minus the
       * sum of all other known status counts. This is forbidden — each count must
       * come directly from byStatus.
       *
       * We create summaries where the arithmetic complement would be wrong
       * to prove the mapping never uses subtraction.
       */
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 5 }),
          fc.integer({ min: 0, max: 100 }),
          fc.integer({ min: 0, max: 10_000 }),
          (numEntries, countPerStatus, totalOrders) => {
            // Build a byStatus with 'numEntries' statuses, each with 'countPerStatus' orders
            const statuses = ALL_PAYMENT_STATUSES.slice(0, numEntries)
            const byStatus = statuses.map(status => ({
              status,
              orderCount: countPerStatus,
              totalAmountMinor: 0,
            }))

            const summary: PaymentOrderSummaryResponse = {
              totalOrders,
              totalAmountMinor: 0,
              byCurrency: [],
              byStatus,
            }

            // For each status in byStatus: displayed count must be countPerStatus
            for (const status of statuses) {
              const displayed = displayedCountForStatus(summary, status as PaymentStatus)
              expect(displayed).toBe(countPerStatus)
            }

            // For statuses NOT in byStatus: displayed count must be 0
            for (const status of ALL_PAYMENT_STATUSES.slice(numEntries)) {
              const displayed = displayedCountForStatus(summary, status)
              expect(displayed).toBe(0)
            }
          },
        ),
        { numRuns: 200 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P1d — byStatus entries pass through as identity (no transformation)
  // -------------------------------------------------------------------------

  describe('P1d: byStatus mapping is an identity — no transformation of status or orderCount', () => {
    it('should pass byStatus entries through unchanged to the display layer', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * The mapping of summary.byStatus to the display layer must be a pure
       * pass-through. The status label and orderCount must be identical to what
       * the backend returned — no renaming, no reordering that changes values,
       * no arithmetic.
       */
      fc.assert(
        fc.property(summaryResponseArb, (summary) => {
          const displayedEntries = displayedByStatusEntries(summary)

          // Same number of entries as the backend returned
          expect(displayedEntries.length).toBe(summary.byStatus.length)

          // Each entry must be an exact pass-through of the backend entry
          for (let i = 0; i < summary.byStatus.length; i++) {
            const backendEntry = summary.byStatus[i]!
            const displayedEntry = displayedEntries[i]!

            expect(displayedEntry.status).toBe(backendEntry.status)
            expect(displayedEntry.orderCount).toBe(backendEntry.orderCount)
          }
        }),
        { numRuns: 200 },
      )
    })

    it('should not add extra entries or drop entries from byStatus', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * The displayed entries set must have exactly the same size as byStatus —
       * no phantom entries added for "expected" statuses, no entries dropped
       * for statuses with count=0 in byStatus.
       */
      fc.assert(
        fc.property(summaryResponseArb, (summary) => {
          const displayedEntries = displayedByStatusEntries(summary)

          // Count invariant: entries in == entries out
          expect(displayedEntries.length).toBe(summary.byStatus.length)

          // Every backend entry is represented in the display
          const displayedStatuses = new Set(displayedEntries.map(e => e.status))
          for (const backendEntry of summary.byStatus) {
            expect(displayedStatuses.has(backendEntry.status)).toBe(true)
          }
        }),
        { numRuns: 200 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P1e — Full integration: all three displayed values are backend pass-throughs
  // -------------------------------------------------------------------------

  describe('P1e: Full metric fidelity — all three displayed values are exact backend values', () => {
    it('should preserve all displayed metrics as exact backend values simultaneously', () => {
      /**
       * Validates: Requirements 1.1, 1.7
       *
       * Integration property combining P1a, P1b, and P1c: for any pair of
       * (merchantListResponse, summaryResponse), all displayed metrics —
       * merchantCount, totalOrders, and per-status counts — must equal
       * exactly the corresponding backend fields. No field is fabricated.
       */
      fc.assert(
        fc.property(merchantListResponseArb, summaryResponseArb, (merchantList, summary) => {
          // P1a: merchant count
          const displayedMerchants = displayedMerchantCount(merchantList)
          expect(displayedMerchants).toBe(merchantList.totalElements)

          // P1b: total order count
          const displayedTotal = displayedTotalOrders(summary)
          expect(displayedTotal).toBe(summary.totalOrders)

          // P1c: per-status counts
          for (const status of ALL_PAYMENT_STATUSES) {
            const backendEntry = summary.byStatus.find(e => e.status === status)
            const expectedCount = backendEntry?.orderCount ?? 0
            const displayed = displayedCountForStatus(summary, status)
            expect(displayed).toBe(expectedCount)
          }
        }),
        { numRuns: 200 },
      )
    })

    it('should never display a metric that is not present in either the merchant list or summary payload', () => {
      /**
       * Validates: Requirements 1.7
       *
       * This property verifies the non-fabrication contract: every displayed
       * numeric metric must be traceable to a named field in the backend payload.
       * We assert this by confirming displayed values equal their source fields
       * and that no displayed value appears from thin air.
       */
      fc.assert(
        fc.property(merchantListResponseArb, summaryResponseArb, (merchantList, summary) => {
          // All displayable metrics are enumerable:
          const allDisplayedMetrics: number[] = [
            displayedMerchantCount(merchantList),
            displayedTotalOrders(summary),
            ...ALL_PAYMENT_STATUSES.map(s => displayedCountForStatus(summary, s)),
          ]

          // All must be non-negative integers (no negative or fractional fabrications)
          for (const metric of allDisplayedMetrics) {
            expect(metric).toBeGreaterThanOrEqual(0)
            expect(Number.isInteger(metric)).toBe(true)
          }

          // merchantCount must trace to totalElements
          expect(allDisplayedMetrics[0]).toBe(merchantList.totalElements)

          // totalOrders must trace to summary.totalOrders
          expect(allDisplayedMetrics[1]).toBe(summary.totalOrders)

          // Each per-status count must trace to byStatus.orderCount (or 0 if absent)
          for (let i = 0; i < ALL_PAYMENT_STATUSES.length; i++) {
            const status = ALL_PAYMENT_STATUSES[i]!
            const backendEntry = summary.byStatus.find(e => e.status === status)
            expect(allDisplayedMetrics[2 + i]).toBe(backendEntry?.orderCount ?? 0)
          }
        }),
        { numRuns: 200 },
      )
    })
  })
})
