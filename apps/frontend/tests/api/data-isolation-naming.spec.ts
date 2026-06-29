/**
 * F-A4: Worker-aware data isolation — naming convention validation
 *
 * Validates the deterministic naming strategy for test data isolation.
 * These are pure logic tests (no browser, no backend required) that confirm:
 *   1. Worker prefixes are deterministic per workerIndex.
 *   2. Prefixes differ across workers (no cross-worker collision).
 *   3. Merchant and payment refs follow the agreed format.
 *
 * Why this matters: enabling fullyParallel requires that every mutation test
 * uses worker-prefixed identifiers. Validating the naming function here ensures
 * the convention is enforced without magic strings scattered across tests.
 *
 * Seed/reset API: resetTestData() and seedTestData() are implemented in
 * tests/support/test-data-isolation.ts but BLOCKED in CI (backend not started).
 * See that file for the enablement conditions.
 */

import { test, expect } from '@playwright/test'
import {
  workerPrefix,
  isolatedMerchantRef,
  isolatedPaymentRef,
} from '../support/test-data-isolation'

test.describe('F-A4: Worker naming convention validation', () => {
  test('workerPrefix is deterministic per workerIndex', () => {
    expect(workerPrefix(0)).toBe('W0')
    expect(workerPrefix(1)).toBe('W1')
    expect(workerPrefix(2)).toBe('W2')
  })

  test('workerPrefix differs across workers (no collision)', () => {
    const prefixes = [0, 1, 2, 3].map(workerPrefix)
    const unique = new Set(prefixes)
    expect(unique.size).toBe(prefixes.length)
  })

  test('isolatedMerchantRef produces worker-scoped unique references', () => {
    const ref0 = isolatedMerchantRef(0, 'CREATE')
    const ref1 = isolatedMerchantRef(1, 'CREATE')

    expect(ref0).toBe('TEST-W0-CREATE')
    expect(ref1).toBe('TEST-W1-CREATE')
    expect(ref0).not.toBe(ref1)
  })

  test('isolatedPaymentRef produces worker-scoped unique references', () => {
    const ref0 = isolatedPaymentRef(0, 'LIFECYCLE')
    const ref1 = isolatedPaymentRef(1, 'LIFECYCLE')

    expect(ref0).toBe('PAY-W0-LIFECYCLE')
    expect(ref1).toBe('PAY-W1-LIFECYCLE')
    expect(ref0).not.toBe(ref1)
  })

  test('refs from same worker are stable across multiple calls', () => {
    const a = isolatedMerchantRef(0, 'STABLE')
    const b = isolatedMerchantRef(0, 'STABLE')
    expect(a).toBe(b)
  })
})
