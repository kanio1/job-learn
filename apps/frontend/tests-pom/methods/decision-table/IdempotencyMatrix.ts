/**
 * DT — Idempotency-Key × body on POST payment-order.
 *
 * What changes: key reuse and body equality. Not the merchant.
 * Layer: REST. Replay/conflict are Spring; absent/empty key is BFF mint (201).
 * Seed: unique factory on ALPHA_001. Never seed-learning.
 */

export type IdempotencyKeyMode = 'present' | 'absent' | 'empty'

export type IdempotencyRow = {
  id: string
  sameKey: boolean
  sameBody: boolean
  keyMode?: IdempotencyKeyMode
  expectStatus: 201 | 200 | 409 | 400
  error?: 'idempotency_conflict'
}

export const idempotencyMatrix: readonly IdempotencyRow[] = [
  { id: 'SCN-PAY-01', sameKey: false, sameBody: true, expectStatus: 201 },
  { id: 'SCN-PAY-02', sameKey: true, sameBody: true, expectStatus: 200 },
  { id: 'SCN-PAY-03', sameKey: true, sameBody: false, expectStatus: 409, error: 'idempotency_conflict' },
  // BFF mints `idem-${Date.now()}-…` when the header is missing/blank (Spring create is 400).
  { id: 'SCN-PAY-04', sameKey: false, sameBody: true, keyMode: 'absent', expectStatus: 201 },
  { id: 'SCN-PAY-05', sameKey: false, sameBody: true, keyMode: 'empty', expectStatus: 201 },
]

export const idempotencyKeyBoundaries = idempotencyMatrix.filter(row => row.keyMode && row.keyMode !== 'present')
