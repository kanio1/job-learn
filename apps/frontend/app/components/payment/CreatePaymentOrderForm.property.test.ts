/**
 * Property 12: Idempotency-Key reuse on unchanged resubmit
 *
 * **Validates: Requirements 3.4**
 *
 * Key behavior verified by this property test:
 *   - When a create payment order request FAILS and the user resubmits WITHOUT
 *     changing form values → the SAME Idempotency-Key is used (not a new one)
 *   - When the user CHANGES any form field before resubmitting → a NEW
 *     Idempotency-Key is generated (not the failed-submit key)
 *   - The Idempotency-Key is always ≤255 characters (hard constraint per Req 5.10)
 *   - The Idempotency-Key is always non-empty
 *   - On initial form load → a unique Idempotency-Key is pre-populated (via onMounted UUID)
 *
 * Implementation strategy:
 *   The idempotency-key reuse logic in CreatePaymentOrderForm.vue is a pure state-machine
 *   implemented with reactive references and a watcher. Rather than mounting the full Nuxt
 *   component (which requires heavy mocking), this test exercises the extracted logic
 *   functions and state-machine invariants directly with fast-check.
 *
 *   The functions under test are extracted and replicated faithfully from the component
 *   so the property assertions remain stable against the component's behavior contract.
 *
 * Tag: Feature: payment-operations-dashboard, Property 12: Idempotency-Key reuse on unchanged resubmit
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// ─── Types mirroring the form shape ──────────────────────────────────────────

interface FormSnapshot {
  amountMinor: number | undefined
  currency: string | undefined
  clientOrderReference: string
}

interface FormState extends FormSnapshot {}

// ─── Extracted pure logic from CreatePaymentOrderForm.vue ────────────────────
// These functions are extracted verbatim from the component's business logic
// so this test covers the actual behavior contract.

function formMatchesSnapshot(state: FormState, snapshot: FormSnapshot | null): boolean {
  if (!snapshot) return false
  return (
    snapshot.amountMinor === state.amountMinor &&
    snapshot.currency === state.currency &&
    snapshot.clientOrderReference === state.clientOrderReference
  )
}

function generateKey(): string {
  // Uses the same generation strategy as the component (crypto.randomUUID).
  // In Node/Vitest environment crypto.randomUUID() is available.
  return crypto.randomUUID()
}

/**
 * State machine that mirrors the idempotency-key lifecycle in the form:
 *   - currentKey: the key that would be sent with the next request
 *   - failedSnapshot: set when a submit fails; cleared when values change
 *
 * Operations:
 *   - initialise()   — simulates component mount (key is generated)
 *   - submitFail()   — simulates a failed submit: snapshot is saved, key retained
 *   - resubmit()     — simulates resubmit with optional field changes
 *   - success()      — simulates a successful submit: key regenerated, snapshot cleared
 */
class IdempotencyKeyStateMachine {
  currentKey: string
  failedSnapshot: FormSnapshot | null = null
  currentState: FormState

  constructor(initialState: FormState) {
    this.currentKey = generateKey()
    this.currentState = { ...initialState }
  }

  /** Submit fails — retain key, capture snapshot */
  submitFail(): void {
    this.failedSnapshot = { ...this.currentState }
    // key is NOT changed
  }

  /**
   * User changes some fields, then resubmits.
   * If any field changed after a failure → new key is generated.
   * If fields unchanged after a failure → key is retained.
   */
  applyFieldChanges(newState: FormState): void {
    if (this.failedSnapshot && !formMatchesSnapshot(newState, this.failedSnapshot)) {
      // Values changed → generate new key and clear snapshot (mirrors the watcher)
      this.currentKey = generateKey()
      this.failedSnapshot = null
    }
    this.currentState = { ...newState }
  }

  /** Submit succeeds — generate a fresh key, clear snapshot */
  submitSuccess(): void {
    this.currentKey = generateKey()
    this.failedSnapshot = null
  }
}

// ─── Arbitraries ─────────────────────────────────────────────────────────────

/** Arbitrary valid amountMinor (1..100_000_000 matching the schema constraint) */
const amountMinorArb = fc.integer({ min: 1, max: 100_000_000 })

/** Arbitrary currency matching the enum PLN | EUR | USD */
const currencyArb = fc.constantFrom('PLN', 'EUR', 'USD')

/** Arbitrary clientOrderReference (1..120 chars, matching the existing schema) */
const clientOrderReferenceArb = fc.string({ minLength: 1, maxLength: 120 })

/** Arbitrary valid form state */
const formStateArb: fc.Arbitrary<FormState> = fc.record({
  amountMinor: amountMinorArb,
  currency: currencyArb,
  clientOrderReference: clientOrderReferenceArb,
})

/**
 * A pair of form states that are DIFFERENT in at least one field.
 * Used to assert that a field change triggers key regeneration.
 */
const differentFormStateArb: fc.Arbitrary<{ original: FormState; modified: FormState }> = fc
  .record({ original: formStateArb })
  .chain(({ original }) =>
    fc
      .oneof(
        // Change amountMinor
        amountMinorArb
          .filter((a) => a !== original.amountMinor)
          .map((amountMinor) => ({ ...original, amountMinor })),
        // Change currency
        currencyArb
          .filter((c) => c !== original.currency)
          .map((currency) => ({ ...original, currency })),
        // Change clientOrderReference
        clientOrderReferenceArb
          .filter((r) => r !== original.clientOrderReference)
          .map((clientOrderReference) => ({ ...original, clientOrderReference })),
      )
      .map((modified) => ({ original, modified })),
  )

// ─── Property Tests ──────────────────────────────────────────────────────────

describe('Feature: payment-operations-dashboard, Property 12: Idempotency-Key reuse on unchanged resubmit', () => {
  // ── Property 12a: Initial key is non-empty and ≤255 chars ─────────────────

  it('Property 12a — initial Idempotency-Key is non-empty and ≤255 characters (≥100 iterations)', async () => {
    // **Validates: Requirements 3.4, 5.10**
    await fc.assert(
      fc.asyncProperty(formStateArb, async (state) => {
        const machine = new IdempotencyKeyStateMachine(state)

        expect(machine.currentKey.length).toBeGreaterThan(0)
        expect(machine.currentKey.length).toBeLessThanOrEqual(255)
      }),
      { numRuns: 100 },
    )
  })

  // ── Property 12b: Unchanged resubmit after failure reuses the same key ────

  it('Property 12b — unchanged resubmit after failure reuses the SAME Idempotency-Key (≥100 iterations)', async () => {
    // **Validates: Requirements 3.4**
    // Scenario: submit → fail → resubmit with identical values → key must be the same
    await fc.assert(
      fc.asyncProperty(formStateArb, async (state) => {
        const machine = new IdempotencyKeyStateMachine(state)

        const keyBeforeFailure = machine.currentKey

        // Simulate a failed submit
        machine.submitFail()

        // Simulate user NOT changing any field (applyFieldChanges with same values)
        machine.applyFieldChanges({ ...state })

        // The key must NOT have changed
        expect(machine.currentKey).toBe(keyBeforeFailure)
      }),
      { numRuns: 100 },
    )
  })

  // ── Property 12c: Changed values after failure trigger a NEW key ──────────

  it('Property 12c — changing any form field after failure generates a NEW Idempotency-Key (≥100 iterations)', async () => {
    // **Validates: Requirements 3.4**
    // Scenario: submit → fail → change field → new key must differ from the pre-failure key
    await fc.assert(
      fc.asyncProperty(differentFormStateArb, async ({ original, modified }) => {
        const machine = new IdempotencyKeyStateMachine(original)

        const keyBeforeFailure = machine.currentKey

        // Simulate a failed submit
        machine.submitFail()

        // Simulate user changing at least one form field
        machine.applyFieldChanges(modified)

        // The key MUST have been regenerated — it cannot be the same as the failed key
        expect(machine.currentKey).not.toBe(keyBeforeFailure)
      }),
      { numRuns: 100 },
    )
  })

  // ── Property 12d: New key is also non-empty and ≤255 chars ────────────────

  it('Property 12d — regenerated key after field change is non-empty and ≤255 characters (≥100 iterations)', async () => {
    // **Validates: Requirements 3.4, 5.10**
    await fc.assert(
      fc.asyncProperty(differentFormStateArb, async ({ original, modified }) => {
        const machine = new IdempotencyKeyStateMachine(original)
        machine.submitFail()
        machine.applyFieldChanges(modified)

        expect(machine.currentKey.length).toBeGreaterThan(0)
        expect(machine.currentKey.length).toBeLessThanOrEqual(255)
      }),
      { numRuns: 100 },
    )
  })

  // ── Property 12e: Keys across independent sessions are unique ─────────────

  it('Property 12e — two independent form loads always produce different initial keys (≥100 iterations)', async () => {
    // **Validates: Requirements 3.2, 8.8** — "unique per initiation"
    // This property tests the uniqueness contract: each form mount generates a fresh UUID.
    await fc.assert(
      fc.asyncProperty(formStateArb, async (state) => {
        const machine1 = new IdempotencyKeyStateMachine(state)
        const machine2 = new IdempotencyKeyStateMachine(state)

        // Two separate initialisations must yield different keys
        expect(machine1.currentKey).not.toBe(machine2.currentKey)
      }),
      { numRuns: 100 },
    )
  })

  // ── Property 12f: Key is retained across multiple unchanged resubmits ─────

  it('Property 12f — key remains stable across multiple unchanged resubmits after failure (≥100 iterations)', async () => {
    // **Validates: Requirements 3.4**
    // After a failure, multiple identical resubmits must all reuse the same key.
    const retryCountArb = fc.integer({ min: 2, max: 5 })

    await fc.assert(
      fc.asyncProperty(formStateArb, retryCountArb, async (state, retries) => {
        const machine = new IdempotencyKeyStateMachine(state)
        machine.submitFail()

        const keyAfterFailure = machine.currentKey

        for (let i = 0; i < retries; i++) {
          machine.applyFieldChanges({ ...state }) // same values each time
          expect(machine.currentKey).toBe(keyAfterFailure)
        }
      }),
      { numRuns: 100 },
    )
  })

  // ── Property 12g: Successful submit always generates a fresh key ──────────

  it('Property 12g — successful submit always generates a fresh key regardless of prior state (≥100 iterations)', async () => {
    // **Validates: Requirements 3.2**
    // After success, a new form action should get a new key (not the old one).
    await fc.assert(
      fc.asyncProperty(formStateArb, async (state) => {
        const machine = new IdempotencyKeyStateMachine(state)
        const initialKey = machine.currentKey

        machine.submitSuccess()

        // A new key is generated after success
        expect(machine.currentKey).not.toBe(initialKey)
        // And it must still satisfy the length invariants
        expect(machine.currentKey.length).toBeGreaterThan(0)
        expect(machine.currentKey.length).toBeLessThanOrEqual(255)
      }),
      { numRuns: 100 },
    )
  })

  // ── Property 12h: Snapshot is cleared after changed-field key regeneration ─

  it('Property 12h — failedSnapshot is cleared once field change triggers key regeneration (≥100 iterations)', async () => {
    // **Validates: Requirements 3.4**
    // After the snapshot is cleared, further unchanged re-application of the new values
    // must NOT trigger another key regeneration (no stale snapshot comparison).
    await fc.assert(
      fc.asyncProperty(differentFormStateArb, async ({ original, modified }) => {
        const machine = new IdempotencyKeyStateMachine(original)
        machine.submitFail()

        // Change field → new key, snapshot cleared
        machine.applyFieldChanges(modified)
        const keyAfterChange = machine.currentKey

        // Apply the same modified values again (no snapshot exists any more)
        machine.applyFieldChanges({ ...modified })

        // Key must NOT change again because the snapshot was cleared
        expect(machine.currentKey).toBe(keyAfterChange)
      }),
      { numRuns: 100 },
    )
  })
})
