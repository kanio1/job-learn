/**
 * Property 12: Idempotency-Key reuse on unchanged resubmit
 *
 * Validates: Requirements 3.4
 *
 * Tag: Feature: payment-operations-dashboard, Property 12: Idempotency-Key reuse on unchanged resubmit
 *
 * The property under test — extracted from CreatePaymentOrderForm.vue:
 *
 *   P12a — When a create attempt fails and the user resubmits with UNCHANGED form
 *           values, the same Idempotency-Key is reused.
 *
 *   P12b — When a create attempt fails and the user changes ANY form field before
 *           resubmitting, a NEW Idempotency-Key is generated (different from the
 *           previous one).
 *
 *   P12c — The Idempotency-Key is always non-empty.
 *
 *   P12d — The Idempotency-Key is always ≤ 255 characters.
 *
 * Test approach: the pure state-transition logic from the component is modelled
 * directly here without mounting the component. The three core functions extracted:
 *
 *   generateKey()            → crypto.randomUUID()
 *   formMatchesSnapshot(f, s) → true iff every field value is equal
 *   resolveKey(key, changed)  → same key if !changed, new key if changed
 *
 * No DOM mount, no Nuxt context required.
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// ---------------------------------------------------------------------------
// Types mirroring the form state in CreatePaymentOrderForm.vue
// ---------------------------------------------------------------------------

interface FormState {
  amountMinor: number | undefined
  currency: 'PLN' | 'EUR' | 'USD' | undefined
  clientOrderReference: string
}

type FailedSubmitSnapshot = FormState

// ---------------------------------------------------------------------------
// Pure logic extracted from CreatePaymentOrderForm.vue
//
// These functions encapsulate the exact idempotency-key lifecycle rules that
// the component implements.
// ---------------------------------------------------------------------------

/**
 * Generates a new unique Idempotency-Key.
 * Mirrors the component's: `idempotencyKey.value = crypto.randomUUID()`
 */
function generateKey(): string {
  return crypto.randomUUID()
}

/**
 * Returns true when current form state exactly matches the last-failed snapshot.
 * Mirrors the component's `formMatchesFailedSnapshot()`.
 */
function formMatchesSnapshot(form: FormState, snapshot: FailedSubmitSnapshot): boolean {
  return (
    form.amountMinor === snapshot.amountMinor &&
    form.currency === snapshot.currency &&
    form.clientOrderReference === snapshot.clientOrderReference
  )
}

/**
 * Resolves which key to use on a resubmit after failure:
 *
 *  - If form is unchanged (matches snapshot)  → reuse the existing key.
 *  - If any form field changed                → generate a fresh key.
 *
 * Mirrors the watcher + submit logic in CreatePaymentOrderForm.vue:
 *   watch(formFields, () => {
 *     if (failedSnapshot && !formMatchesFailedSnapshot()) {
 *       idempotencyKey = crypto.randomUUID()
 *       failedSnapshot = null
 *     }
 *   })
 */
function resolveKeyAfterFailure(
  currentKey: string,
  form: FormState,
  snapshot: FailedSubmitSnapshot,
): { key: string; isReused: boolean } {
  if (formMatchesSnapshot(form, snapshot)) {
    // Unchanged — reuse the same key
    return { key: currentKey, isReused: true }
  }
  // Changed — generate a new key
  return { key: generateKey(), isReused: false }
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

const currencyArb = fc.constantFrom<'PLN' | 'EUR' | 'USD'>('PLN', 'EUR', 'USD')

const amountMinorArb = fc.integer({ min: 1, max: 100_000_000 })

const clientOrderRefArb = fc.string({ minLength: 1, maxLength: 120 })

/** Generates a valid, non-empty form state */
const formStateArb: fc.Arbitrary<FormState> = fc.record({
  amountMinor: amountMinorArb,
  currency: currencyArb,
  clientOrderReference: clientOrderRefArb,
})

/**
 * Generates a pair of form states where at least one field differs.
 * Used to drive P12b (form values changed → new key).
 */
const changedFormArb: fc.Arbitrary<{ original: FormState; changed: FormState }> = fc
  .tuple(formStateArb, formStateArb)
  .filter(([a, b]) => !formMatchesSnapshot(a, b))
  .map(([original, changed]) => ({ original, changed }))

/**
 * Generates a UUID-like key via crypto.randomUUID(); represented as a string
 * for the property that checks length/non-empty invariants.
 */
const keyArb: fc.Arbitrary<string> = fc.constant(null).map(() => generateKey())

// ---------------------------------------------------------------------------
// P12a — Unchanged resubmit reuses the same Idempotency-Key
// ---------------------------------------------------------------------------

describe('Feature: payment-operations-dashboard, Property 12: Idempotency-Key reuse on unchanged resubmit', () => {
  describe('P12a: Unchanged resubmit reuses the same Idempotency-Key', () => {
    it('should reuse the same key when form values are identical to the failed-submit snapshot', () => {
      /**
       * Validates: Requirements 3.4
       *
       * ∀ form state F, current key K:
       *   if the user submits again with form state === F (the snapshot),
       *   the resolved key is still K (reused).
       */
      fc.assert(
        fc.property(formStateArb, keyArb, (form, currentKey) => {
          // The snapshot captures the exact form state that failed
          const snapshot: FailedSubmitSnapshot = { ...form }

          // The user resubmits with identical values (no changes)
          const { key: resolvedKey, isReused } = resolveKeyAfterFailure(currentKey, form, snapshot)

          // Property: the same key must be reused
          expect(isReused).toBe(true)
          expect(resolvedKey).toBe(currentKey)
        }),
        { numRuns: 100 },
      )
    })

    it('should reuse the key across multiple identical resubmits', () => {
      /**
       * If the user submits multiple times without changing the form,
       * the key remains the same throughout all retries.
       */
      fc.assert(
        fc.property(formStateArb, keyArb, fc.integer({ min: 2, max: 10 }), (form, initialKey, retries) => {
          const snapshot: FailedSubmitSnapshot = { ...form }
          let currentKey = initialKey

          for (let i = 0; i < retries; i++) {
            const { key, isReused } = resolveKeyAfterFailure(currentKey, form, snapshot)
            expect(isReused).toBe(true)
            expect(key).toBe(initialKey)
            currentKey = key
          }
        }),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P12b — Changed form values trigger a new Idempotency-Key
  // -------------------------------------------------------------------------

  describe('P12b: Changed form values trigger a new Idempotency-Key', () => {
    it('should generate a new key when any form field changes after a failure', () => {
      /**
       * Validates: Requirements 3.4
       *
       * ∀ form-change pair (original, changed) where original ≠ changed:
       *   If the failure snapshot captured `original` and the user edits at least
       *   one field to produce `changed`, the resolved key must be different from
       *   the original key (a fresh key is generated).
       */
      fc.assert(
        fc.property(changedFormArb, keyArb, ({ original, changed }, currentKey) => {
          // Snapshot was captured from the original failed submission
          const snapshot: FailedSubmitSnapshot = { ...original }

          // User has changed at least one field
          const { key: resolvedKey, isReused } = resolveKeyAfterFailure(currentKey, changed, snapshot)

          // Property: a new key must be generated (not the same as the old one)
          expect(isReused).toBe(false)
          expect(resolvedKey).not.toBe(currentKey)
        }),
        { numRuns: 100 },
      )
    })

    it('should generate a new key when amountMinor is changed', () => {
      /**
       * Individual field coverage: changing amountMinor alone triggers new key.
       */
      fc.assert(
        fc.property(
          formStateArb,
          keyArb,
          fc.integer({ min: 1, max: 100_000_000 }),
          (form, currentKey, newAmount) => {
            // Ensure the new amount is different
            fc.pre(newAmount !== form.amountMinor)

            const snapshot: FailedSubmitSnapshot = { ...form }
            const changedForm: FormState = { ...form, amountMinor: newAmount }

            const { isReused, key } = resolveKeyAfterFailure(currentKey, changedForm, snapshot)

            expect(isReused).toBe(false)
            expect(key).not.toBe(currentKey)
          },
        ),
        { numRuns: 100 },
      )
    })

    it('should generate a new key when currency is changed', () => {
      /**
       * Individual field coverage: changing currency alone triggers new key.
       */
      fc.assert(
        fc.property(formStateArb, keyArb, (form, currentKey) => {
          // Pick a currency different from the current one
          const otherCurrencies = (['PLN', 'EUR', 'USD'] as const).filter(c => c !== form.currency)
          if (otherCurrencies.length === 0) return // all same, skip

          const newCurrency = otherCurrencies[0]!
          const snapshot: FailedSubmitSnapshot = { ...form }
          const changedForm: FormState = { ...form, currency: newCurrency }

          const { isReused, key } = resolveKeyAfterFailure(currentKey, changedForm, snapshot)

          expect(isReused).toBe(false)
          expect(key).not.toBe(currentKey)
        }),
        { numRuns: 100 },
      )
    })

    it('should generate a new key when clientOrderReference is changed', () => {
      /**
       * Individual field coverage: changing clientOrderReference alone triggers new key.
       */
      fc.assert(
        fc.property(
          formStateArb,
          keyArb,
          fc.string({ minLength: 1, maxLength: 120 }),
          (form, currentKey, newRef) => {
            fc.pre(newRef !== form.clientOrderReference)

            const snapshot: FailedSubmitSnapshot = { ...form }
            const changedForm: FormState = { ...form, clientOrderReference: newRef }

            const { isReused, key } = resolveKeyAfterFailure(currentKey, changedForm, snapshot)

            expect(isReused).toBe(false)
            expect(key).not.toBe(currentKey)
          },
        ),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P12c — Idempotency-Key is always non-empty
  // -------------------------------------------------------------------------

  describe('P12c: Idempotency-Key is always non-empty', () => {
    it('should generate a non-empty key on initial creation', () => {
      /**
       * Validates: Requirements 3.4, 8.8
       *
       * crypto.randomUUID() always produces a non-empty UUID string.
       * This property runs ≥ 100 times to confirm no empty key is ever generated.
       */
      fc.assert(
        fc.property(fc.constant(null), () => {
          const key = generateKey()
          expect(key).toBeTruthy()
          expect(key.length).toBeGreaterThan(0)
        }),
        { numRuns: 100 },
      )
    })

    it('should produce a non-empty key when changing form values triggers regeneration', () => {
      fc.assert(
        fc.property(changedFormArb, keyArb, ({ original, changed }, currentKey) => {
          const snapshot: FailedSubmitSnapshot = { ...original }
          const { key: resolvedKey } = resolveKeyAfterFailure(currentKey, changed, snapshot)

          expect(resolvedKey).toBeTruthy()
          expect(resolvedKey.length).toBeGreaterThan(0)
        }),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P12d — Idempotency-Key is always ≤ 255 characters
  // -------------------------------------------------------------------------

  describe('P12d: Idempotency-Key is always ≤ 255 characters', () => {
    it('should generate a key with at most 255 characters on initial creation', () => {
      /**
       * Validates: Requirements 3.4, 5.10, 8.8
       *
       * The IdempotencyKeyInput component uses crypto.randomUUID() which produces
       * a 36-character UUID (format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx).
       * This property asserts the length invariant holds for ≥ 100 generated keys.
       */
      fc.assert(
        fc.property(fc.constant(null), () => {
          const key = generateKey()
          expect(key.length).toBeLessThanOrEqual(255)
        }),
        { numRuns: 100 },
      )
    })

    it('should not exceed 255 characters when a new key is generated after form change', () => {
      fc.assert(
        fc.property(changedFormArb, keyArb, ({ original, changed }, currentKey) => {
          const snapshot: FailedSubmitSnapshot = { ...original }
          const { key: resolvedKey } = resolveKeyAfterFailure(currentKey, changed, snapshot)

          expect(resolvedKey.length).toBeLessThanOrEqual(255)
        }),
        { numRuns: 100 },
      )
    })
  })

  // -------------------------------------------------------------------------
  // P12 integration: full failure → resubmit sequence
  // -------------------------------------------------------------------------

  describe('P12 integration: failure → resubmit sequence', () => {
    it('should correctly manage the key lifecycle across a sequence of failures and form changes', () => {
      /**
       * Simulates the full lifecycle described in Requirements 3.4:
       *
       * For each step in a generated sequence:
       *   - A form state is submitted and fails
       *   - A snapshot is taken
       *   - Either the same values are resubmitted (key reused) OR
       *     the values are changed (new key generated)
       *
       * Invariants that must hold throughout:
       *   1. Same values → same key
       *   2. Changed values → new key (≠ previous key)
       *   3. All keys are non-empty and ≤ 255 chars
       */
      const stepArb = fc.record({
        form: formStateArb,
        changesValues: fc.boolean(),
        changedForm: formStateArb,
      })

      fc.assert(
        fc.property(
          fc.array(stepArb, { minLength: 1, maxLength: 8 }),
          (steps) => {
            let currentKey = generateKey()

            for (const step of steps) {
              const snapshot: FailedSubmitSnapshot = { ...step.form }

              if (!step.changesValues || formMatchesSnapshot(step.form, step.changedForm)) {
                // Resubmit with same values — key must be reused
                const { key, isReused } = resolveKeyAfterFailure(currentKey, step.form, snapshot)
                expect(isReused).toBe(true)
                expect(key).toBe(currentKey)
                // Key invariants
                expect(key.length).toBeGreaterThan(0)
                expect(key.length).toBeLessThanOrEqual(255)
              } else {
                // Resubmit with changed values — new key must be generated
                const { key, isReused } = resolveKeyAfterFailure(currentKey, step.changedForm, snapshot)
                expect(isReused).toBe(false)
                expect(key).not.toBe(currentKey)
                // Key invariants
                expect(key.length).toBeGreaterThan(0)
                expect(key.length).toBeLessThanOrEqual(255)
                currentKey = key
              }
            }
          },
        ),
        { numRuns: 100 },
      )
    })
  })
})
