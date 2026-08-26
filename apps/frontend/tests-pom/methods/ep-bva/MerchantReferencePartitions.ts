/**
 * EP/BVA — merchant reference / name (UI Zod + server uniqueness).
 *
 * UI empty form: 0 POST (BVA-W2-020). Duplicate: 409 REST (E2E-026).
 * Missing tenantReference as platform: 400 REST. UI: platform sees tenant field; tenant.admin uses JWT.
 */

export const merchantReferencePartitions = {
  emptyUi: { id: 'SCN-MER-05', expectPostCount: 0 },
  duplicate: { id: 'SCN-MER-06', expectStatus: 409, error: 'duplicate_merchant_reference' },
  platformMissingTenant: { id: 'SCN-MER-09', expectStatus: 400 },
} as const

export type MerchantReferenceLengthRow = {
  id: string
  length: number
  expectStatus: 201 | 400
}

/** Backend + Zod: 3..64, start/end alphanumeric (`MerchantReference`). */
export const merchantReferenceLengthPartitions: readonly MerchantReferenceLengthRow[] = [
  { id: 'SCN-MER-07', length: 2, expectStatus: 400 },
  { id: 'SCN-MER-11', length: 3, expectStatus: 201 },
  { id: 'SCN-MER-08', length: 64, expectStatus: 201 },
  { id: 'SCN-MER-10', length: 65, expectStatus: 400 },
]

export function merchantReferenceForLength(length: number, uniqueSeed: string): string {
  const seed = uniqueSeed.replace(/[^A-Za-z0-9]/g, '').toUpperCase() || 'X'
  if (length <= 0) {
    return ''
  }
  const token = seed.match(/[A-F0-9]{8,}/)?.[0] ?? seed
  const core = token.slice(-Math.min(token.length, length))
  const padded = (core + 'X'.repeat(length)).slice(0, length)
  return padded.replace(/[^A-Z0-9]$/, 'Z').replace(/^[^A-Z0-9]/, 'A')
}
