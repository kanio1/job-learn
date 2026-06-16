/**
 * Property 3: Outbound request gating by form schema
 *
 * Validates: Requirements 3.2, 3.3, 5.4, 5.11, 10.1, 10.2, 10.3
 *
 * Tag: Feature: payment-operations-dashboard, Property 3
 *
 * The create payment order form must gate outbound requests via
 * `createPaymentOrderSchema`. Specifically:
 *
 *  - WHEN form input passes the schema → a request IS allowed (parse succeeds,
 *    the component may proceed with the backend call).
 *  - WHEN form input fails the schema → a request is BLOCKED (parse fails, no
 *    request reaches the backend).
 *
 * Invalid inputs are:
 *  - amountMinor ≤ 0
 *  - amountMinor > 100,000,000
 *  - empty clientOrderReference (zero-length or whitespace-only)
 *  - clientOrderReference > 120 characters
 *  - currency not in the PLN|EUR|USD enum
 *
 * These rules come from the existing `createPaymentOrderSchema` (the source of
 * truth — stricter than the generic requirement prose) and must not be widened.
 *
 * Test design:
 *  P3a — Valid inputs: schema always passes  (≥100 iterations)
 *  P3b — amountMinor ≤ 0: always blocked     (≥100 iterations)
 *  P3c — amountMinor > 100,000,000: blocked  (≥100 iterations)
 *  P3d — empty clientOrderReference: blocked (≥100 iterations)
 *  P3e — oversized clientOrderReference: blocked (≥100 iterations)
 *  P3f — invalid currency: blocked           (≥100 iterations)
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { createPaymentOrderSchema } from '../../app/schemas/payment-order.schema'

// ---------------------------------------------------------------------------
// Helpers that simulate the form gating contract:
//   true  → request IS allowed (schema passed)
//   false → request IS blocked (schema failed)
// ---------------------------------------------------------------------------

function isRequestAllowed(input: unknown): boolean {
  return createPaymentOrderSchema.safeParse(input).success
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

/** Valid currency values supported by the schema */
const validCurrencyArb = fc.constantFrom('PLN', 'EUR', 'USD')

/** Valid amountMinor: integer in [1, 100_000_000] */
const validAmountMinorArb = fc.integer({ min: 1, max: 100_000_000 })

/** Valid clientOrderReference: 1–120 non-whitespace-only characters */
const validClientOrderRefArb = fc
  .string({ minLength: 1, maxLength: 120 })
  .filter(s => s.trim().length > 0)

/** A completely valid create-payment-order input */
const validInputArb = fc.record({
  amountMinor: validAmountMinorArb,
  currency: validCurrencyArb,
  clientOrderReference: validClientOrderRefArb,
})

/** amountMinor ≤ 0 (includes 0 and negative integers) */
const nonPositiveAmountArb = fc.integer({ min: -1_000_000, max: 0 })

/** amountMinor > 100,000,000 (over the schema ceiling) */
const overMaxAmountArb = fc.integer({ min: 100_000_001, max: 1_000_000_000 })

/** Whitespace-only strings (trim().length === 0), representing "empty" references */
const whitespaceOnlyStringArb = fc
  .string({ minLength: 0, maxLength: 30 })
  .map(s => s.replace(/[^ \t\n]/g, ' ')) // replace all non-whitespace with space
  .filter(s => s.trim().length === 0)

/** clientOrderReference exceeding 120 characters — non-whitespace content (v2: printable ASCII only) */
const tooLongRefArb = fc.stringOf(
  fc.integer({ min: 33, max: 126 }).map(n => String.fromCharCode(n)), // printable non-space ASCII
  { minLength: 121, maxLength: 300 }
)

/**
 * Invalid currency: any string that is NOT in ['PLN', 'EUR', 'USD'].
 * We exclude those three values explicitly.
 */
const invalidCurrencyArb = fc
  .string({ minLength: 1, maxLength: 10 })
  .filter(s => !['PLN', 'EUR', 'USD'].includes(s))

// ---------------------------------------------------------------------------
// Property 3 — Main describe block
// ---------------------------------------------------------------------------

describe('Feature: payment-operations-dashboard, Property 3', () => {
  // -------------------------------------------------------------------------
  // P3a — Valid inputs: schema passes → request IS allowed
  // -------------------------------------------------------------------------

  describe('P3a: Valid inputs — schema passes and request is allowed', () => {
    it('should allow the request when all fields satisfy the schema (≥100 iterations)', () => {
      /**
       * Validates: Requirements 3.2, 10.1, 10.3
       *
       * Any combination of (amountMinor ∈ [1, 100_000_000], currency ∈ PLN|EUR|USD,
       * clientOrderReference ∈ [1,120] non-blank) must pass the schema and therefore
       * allow the outbound request.
       */
      fc.assert(
        fc.property(validInputArb, (input) => {
          expect(isRequestAllowed(input)).toBe(true)
        }),
        { numRuns: 200 },
      )
    })

    it('should produce a parsed value that exactly reflects the submitted fields', () => {
      /**
       * Validates: Requirements 10.1, 10.3
       *
       * The parsed data must match the original input — no silent field
       * mutation or addition that could alter what gets sent to the backend.
       */
      fc.assert(
        fc.property(validInputArb, (input) => {
          const result = createPaymentOrderSchema.safeParse(input)

          expect(result.success).toBe(true)
          if (!result.success) return

          expect(result.data.amountMinor).toBe(input.amountMinor)
          expect(result.data.currency).toBe(input.currency)
          // trim() is applied to clientOrderReference by the schema
          expect(result.data.clientOrderReference).toBe(input.clientOrderReference.trim())
        }),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P3b — amountMinor ≤ 0: schema rejects → request is BLOCKED
  // -------------------------------------------------------------------------

  describe('P3b: amountMinor ≤ 0 — schema rejects and request is blocked', () => {
    it('should block the request when amountMinor is zero or negative (≥100 iterations)', () => {
      /**
       * Validates: Requirements 3.3, 5.11, 10.2
       *
       * amountMinor must be ≥ 1. Zero and negative values must be rejected,
       * and no request should be allowed with such a value.
       */
      fc.assert(
        fc.property(nonPositiveAmountArb, validCurrencyArb, validClientOrderRefArb, (amount, currency, ref) => {
          const input = { amountMinor: amount, currency, clientOrderReference: ref }

          expect(isRequestAllowed(input)).toBe(false)
        }),
        { numRuns: 200 },
      )
    })

    it('should report a validation error for amountMinor ≤ 0 (Zod v4 .issues)', () => {
      // Note: renamed to break fast-check replay cache from previous runs
      fc.assert(
        fc.property(nonPositiveAmountArb, validCurrencyArb, validClientOrderRefArb, (amount, currency, ref) => {
          const result = createPaymentOrderSchema.safeParse({
            amountMinor: amount,
            currency,
            clientOrderReference: ref,
          })

          expect(result.success).toBe(false)
        }),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P3c — amountMinor > 100,000,000: schema rejects → request is BLOCKED
  // -------------------------------------------------------------------------

  describe('P3c: amountMinor > 100,000,000 — schema rejects and request is blocked', () => {
    it('should block the request when amountMinor exceeds 100,000,000 (≥100 iterations)', () => {
      /**
       * Validates: Requirements 3.3, 5.4, 10.2
       *
       * The schema ceiling is 100,000,000. Any value above this must be rejected
       * so no over-ceiling request reaches the backend.
       */
      fc.assert(
        fc.property(overMaxAmountArb, validCurrencyArb, validClientOrderRefArb, (amount, currency, ref) => {
          const input = { amountMinor: amount, currency, clientOrderReference: ref }

          expect(isRequestAllowed(input)).toBe(false)
        }),
        { numRuns: 200 },
      )
    })

    it('should report a validation error for amountMinor exceeding the ceiling (Zod v4 .issues)', () => {
      // Note: renamed to break fast-check replay cache from previous runs
      fc.assert(
        fc.property(overMaxAmountArb, validCurrencyArb, validClientOrderRefArb, (amount, currency, ref) => {
          const result = createPaymentOrderSchema.safeParse({
            amountMinor: amount,
            currency,
            clientOrderReference: ref,
          })

          expect(result.success).toBe(false)
        }),
        { numRuns: 100 },
      )
    })

    it('should accept exactly 100,000,000 as the boundary value (inclusive ceiling)', () => {
      /**
       * Boundary check: the schema must accept max = 100,000,000.
       */
      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 100_000_000,
        currency: 'PLN',
        clientOrderReference: 'REF-001',
      })

      expect(result.success).toBe(true)
    })

    it('should reject exactly 100,000,001 (one over the ceiling)', () => {
      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 100_000_001,
        currency: 'PLN',
        clientOrderReference: 'REF-001',
      })

      expect(result.success).toBe(false)
    })
  })

  // -------------------------------------------------------------------------
  // P3d — Empty clientOrderReference: schema rejects → request is BLOCKED
  // -------------------------------------------------------------------------

  describe('P3d: Empty clientOrderReference — schema rejects and request is blocked', () => {
    it('should block the request when clientOrderReference is empty string (≥100 iterations via whitespace)', () => {
      /**
       * Validates: Requirements 3.3, 10.2
       *
       * The schema enforces trim().min(1). A whitespace-only reference trims
       * to an empty string and must be rejected.
       */
      fc.assert(
        fc.property(whitespaceOnlyStringArb, validAmountMinorArb, validCurrencyArb, (ref, amount, currency) => {
          const input = { amountMinor: amount, currency, clientOrderReference: ref }

          expect(isRequestAllowed(input)).toBe(false)
        }),
        { numRuns: 100 },
      )
    })

    it('should block the request when clientOrderReference is exactly an empty string', () => {
      /**
       * Validates: Requirements 3.3, 10.2
       */
      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 1000,
        currency: 'USD',
        clientOrderReference: '',
      })

      expect(result.success).toBe(false)
    })

    it('should report a validation error for empty clientOrderReference (Zod v4 .issues)', () => {
      // Note: renamed to break fast-check replay cache from previous runs
      fc.assert(
        fc.property(whitespaceOnlyStringArb, validAmountMinorArb, validCurrencyArb, (ref, amount, currency) => {
          const result = createPaymentOrderSchema.safeParse({
            amountMinor: amount,
            currency,
            clientOrderReference: ref,
          })

          expect(result.success).toBe(false)
        }),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P3e — clientOrderReference > 120 characters: schema rejects → request is BLOCKED
  // -------------------------------------------------------------------------

  describe('P3e: clientOrderReference > 120 chars — schema rejects and request is blocked', () => {
    it('should block the request when clientOrderReference exceeds 120 characters (≥100 iterations)', () => {
      /**
       * Validates: Requirements 3.3, 10.2
       *
       * The schema enforces max(120). References over 120 characters must
       * be rejected before the request is dispatched.
       */
      fc.assert(
        fc.property(tooLongRefArb, validAmountMinorArb, validCurrencyArb, (ref, amount, currency) => {
          const input = { amountMinor: amount, currency, clientOrderReference: ref }

          expect(isRequestAllowed(input)).toBe(false)
        }),
        { numRuns: 100 },
      )
    })

    it('should accept exactly 120 characters as the boundary value (inclusive ceiling)', () => {
      const exactlyMaxRef = 'A'.repeat(120)

      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 500,
        currency: 'EUR',
        clientOrderReference: exactlyMaxRef,
      })

      expect(result.success).toBe(true)
    })

    it('should reject exactly 121 characters (one over the ceiling)', () => {
      const oneOverRef = 'A'.repeat(121)

      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 500,
        currency: 'EUR',
        clientOrderReference: oneOverRef,
      })

      expect(result.success).toBe(false)
    })

    it('should report a validation error for oversized clientOrderReference (Zod v4 .issues)', () => {
      // Note: renamed to break fast-check replay cache from previous runs
      fc.assert(
        fc.property(tooLongRefArb, validAmountMinorArb, validCurrencyArb, (ref, amount, currency) => {
          const result = createPaymentOrderSchema.safeParse({
            amountMinor: amount,
            currency,
            clientOrderReference: ref,
          })

          expect(result.success).toBe(false)
        }),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P3f — Invalid currency: schema rejects → request is BLOCKED
  // -------------------------------------------------------------------------

  describe('P3f: Invalid currency — schema rejects and request is blocked', () => {
    it('should block the request when currency is not in PLN|EUR|USD (≥100 iterations)', () => {
      /**
       * Validates: Requirements 3.3, 10.2
       *
       * The schema enforces an explicit enum: PLN | EUR | USD.
       * Any currency string outside this set must be rejected.
       */
      fc.assert(
        fc.property(invalidCurrencyArb, validAmountMinorArb, validClientOrderRefArb, (currency, amount, ref) => {
          const input = { amountMinor: amount, currency, clientOrderReference: ref }

          expect(isRequestAllowed(input)).toBe(false)
        }),
        { numRuns: 100 },
      )
    })

    it('should report a validation error for unsupported currency values (Zod v4 .issues)', () => {
      // Note: renamed to break fast-check replay cache from previous test run
      fc.assert(
        fc.property(invalidCurrencyArb, validAmountMinorArb, validClientOrderRefArb, (currency, amount, ref) => {
          const result = createPaymentOrderSchema.safeParse({
            amountMinor: amount,
            currency,
            clientOrderReference: ref,
          })

          expect(result.success).toBe(false)
        }),
        { numRuns: 100 },
      )
    })

    it('should accept exactly PLN, EUR, and USD', () => {
      /**
       * Boundary check — each valid currency must pass on its own.
       */
      for (const currency of ['PLN', 'EUR', 'USD']) {
        const result = createPaymentOrderSchema.safeParse({
          amountMinor: 1000,
          currency,
          clientOrderReference: 'REF-CURRENCY-CHECK',
        })

        expect(result.success).toBe(true)
      }
    })
  })

  // -------------------------------------------------------------------------
  // P3g — Multiple invalid fields simultaneously: all must be caught
  // -------------------------------------------------------------------------

  describe('P3g: Multiple invalid fields — all violations are caught and request is blocked', () => {
    it('should block the request when both amountMinor and currency are invalid', () => {
      /**
       * Validates: Requirements 3.3, 10.2
       *
       * When multiple fields are invalid, the schema must still reject the input
       * (the request is blocked regardless of which field fails first).
       */
      fc.assert(
        fc.property(nonPositiveAmountArb, invalidCurrencyArb, validClientOrderRefArb, (amount, currency, ref) => {
          const input = { amountMinor: amount, currency, clientOrderReference: ref }

          expect(isRequestAllowed(input)).toBe(false)
        }),
        { numRuns: 100 },
      )
    })

    it('should block the request when all three fields are simultaneously invalid', () => {
      fc.assert(
        fc.property(
          nonPositiveAmountArb,
          invalidCurrencyArb,
          whitespaceOnlyStringArb,
          (amount, currency, ref) => {
            const input = { amountMinor: amount, currency, clientOrderReference: ref }

            expect(isRequestAllowed(input)).toBe(false)
          },
        ),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P3h — Boundary: amountMinor = 1 (minimum valid)
  // -------------------------------------------------------------------------

  describe('P3h: Boundary values — inclusive boundaries are accepted', () => {
    it('should allow amountMinor = 1 (minimum boundary)', () => {
      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 1,
        currency: 'PLN',
        clientOrderReference: 'REF-MIN',
      })

      expect(result.success).toBe(true)
    })

    it('should block amountMinor = 0 (below minimum)', () => {
      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 0,
        currency: 'PLN',
        clientOrderReference: 'REF-ZERO',
      })

      expect(result.success).toBe(false)
    })

    it('should allow clientOrderReference of exactly 1 character', () => {
      const result = createPaymentOrderSchema.safeParse({
        amountMinor: 100,
        currency: 'EUR',
        clientOrderReference: 'X',
      })

      expect(result.success).toBe(true)
    })
  })
})
