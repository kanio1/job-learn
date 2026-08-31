/**
 * DT-M360-02 — merchant activate If-Match shape.
 *
 * What changes: If-Match absent / stale / fresh. Not merchant status.
 * Layer: REST (RA-M360-051…053, API-041). UI: one SEC-020 Reload journey.
 */

export type MerchantIfMatchRow = {
  id: string
  ifMatch: 'absent' | 'v99' | 'fresh'
  expectStatus: 200 | 412 | 428
  error?: 'precondition_required' | 'merchant_version_mismatch'
  testId: string
}

export const merchantIfMatchMatrix = [
  {
    id: 'DT-M360-02-absent',
    ifMatch: 'absent',
    expectStatus: 428,
    error: 'precondition_required',
    testId: 'RA-M360-051',
  },
  {
    id: 'DT-M360-02-stale',
    ifMatch: 'v99',
    expectStatus: 412,
    error: 'merchant_version_mismatch',
    testId: 'RA-M360-052',
  },
  {
    id: 'DT-M360-02-fresh',
    ifMatch: 'fresh',
    expectStatus: 200,
    testId: 'RA-M360-053',
  },
] as const satisfies readonly MerchantIfMatchRow[]
