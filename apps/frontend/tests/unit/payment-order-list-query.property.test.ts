/**
 * Property 13: Filter parameters are a supported subset and bounded
 *
 * Validates: Requirements 3.5, 3.7
 *
 * Tag: Feature: payment-operations-dashboard, Property 13: Filter parameters are a supported subset and bounded
 *
 * Asserts:
 *  - The set of keys emitted by paymentOrderListQuerySchema.parse() is always a
 *    subset of the supported parameter names.
 *  - The `size` field returned by a successful parse never exceeds 100.
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  paymentOrderListQuerySchema,
  type PaymentOrderListQuery,
} from '../../app/schemas/payment-order.schema'

// ---------------------------------------------------------------------------
// Supported parameter names (source of truth: design.md Filter schema)
// ---------------------------------------------------------------------------
const SUPPORTED_PARAMS = new Set<string>([
  'status',
  'currency',
  'fromDate',
  'toDate',
  'minAmount',
  'maxAmount',
  'clientOrderReference',
  'page',
  'size',
  'sort',
])

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

/** Generates an arbitrary ISO-8601 date string like "2024-01-15" */
const dateStringArb = fc.date({ min: new Date('2000-01-01'), max: new Date('2030-12-31') })
  .map(d => d.toISOString().slice(0, 10))

/** Generates a valid status value */
const statusArb = fc.constantFrom(
  'CREATED',
  'AUTHORIZED',
  'CAPTURED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
)

/** Generates a valid currency value */
const currencyArb = fc.constantFrom('PLN', 'EUR', 'USD')

/** Generates a valid page (non-negative integer) */
const pageArb = fc.integer({ min: 0, max: 1000 })

/** Generates a valid size (1–100, the schema-enforced range) */
const sizeArb = fc.integer({ min: 1, max: 100 })

/** Generates an arbitrary non-empty client order reference */
const clientOrderRefArb = fc.string({ minLength: 1, maxLength: 120 })

/** Generates a valid non-negative amount */
const amountArb = fc.integer({ min: 0, max: 100_000_000 })

/**
 * Generates an arbitrary partial query object that conforms to the schema
 * (all fields optional, values within schema-accepted ranges).
 */
const validQueryArb = fc.record(
  {
    status: fc.option(statusArb, { nil: undefined }),
    currency: fc.option(currencyArb, { nil: undefined }),
    fromDate: fc.option(dateStringArb, { nil: undefined }),
    toDate: fc.option(dateStringArb, { nil: undefined }),
    minAmount: fc.option(amountArb, { nil: undefined }),
    maxAmount: fc.option(amountArb, { nil: undefined }),
    clientOrderReference: fc.option(clientOrderRefArb, { nil: undefined }),
    page: fc.option(pageArb, { nil: undefined }),
    size: fc.option(sizeArb, { nil: undefined }),
    sort: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: undefined }),
  },
  { requiredKeys: [] },
)

// ---------------------------------------------------------------------------
// Properties
// ---------------------------------------------------------------------------

describe('Feature: payment-operations-dashboard, Property 13: Filter parameters are a supported subset and bounded', () => {
  it('should emit only supported parameter keys after parse', () => {
    fc.assert(
      fc.property(validQueryArb, (rawQuery) => {
        // Remove undefined values to simulate what a UI component would pass
        const input = Object.fromEntries(
          Object.entries(rawQuery).filter(([, v]) => v !== undefined),
        )

        const result = paymentOrderListQuerySchema.safeParse(input)

        // The schema should accept valid input
        expect(result.success).toBe(true)
        if (!result.success) return

        const parsed = result.data as Record<string, unknown>
        const emittedKeys = new Set(Object.keys(parsed))

        // Property: every emitted key must be in the supported set
        for (const key of emittedKeys) {
          expect(SUPPORTED_PARAMS.has(key)).toBe(true)
        }
      }),
      { numRuns: 100 },
    )
  })

  it('should never emit a size greater than 100', () => {
    fc.assert(
      fc.property(validQueryArb, (rawQuery) => {
        const input = Object.fromEntries(
          Object.entries(rawQuery).filter(([, v]) => v !== undefined),
        )

        const result = paymentOrderListQuerySchema.safeParse(input)

        expect(result.success).toBe(true)
        if (!result.success) return

        const parsed = result.data as PaymentOrderListQuery

        // Property: size must never exceed 100
        expect(parsed.size).toBeGreaterThanOrEqual(1)
        expect(parsed.size).toBeLessThanOrEqual(100)
      }),
      { numRuns: 100 },
    )
  })

  it('should reject a size over 100 and never emit it', () => {
    /**
     * Validates: Requirements 3.7
     * Any size > 100 submitted to the schema must be rejected (safeParse failure),
     * ensuring that the component layer cannot accidentally emit size > 100.
     */
    fc.assert(
      fc.property(fc.integer({ min: 101, max: 100_000 }), (oversizedSize) => {
        const result = paymentOrderListQuerySchema.safeParse({ size: oversizedSize })

        // The schema must reject size > 100
        expect(result.success).toBe(false)
      }),
      { numRuns: 100 },
    )
  })

  it('should apply defaults (page=0, size=20) when those fields are absent', () => {
    /**
     * Validates: Requirements 3.7 — defaults of page=0 and size=20 must hold.
     * This is an example-style check complementing the property.
     */
    const result = paymentOrderListQuerySchema.safeParse({})

    expect(result.success).toBe(true)
    if (!result.success) return

    expect(result.data.page).toBe(0)
    expect(result.data.size).toBe(20)
  })
})
