/**
 * DT — create merchant form (existing Zod fields only; no VAT/NIP).
 *
 * What changes: reference length / duplicate / missing tenant.
 * Layer: invalid length = E2E (no POST); 3 and 64 happy + duplicate 409 also REST (RA-M360-030).
 * Seed: unique factory. Never Alpha ~104 as owner.
 */

export type MerchantCreateFormRow = {
  id: string
  referenceLength?: number
  expectStatus: 201 | 400 | 409
  layer: 'e2e' | 'rest' | 'e2e+rest'
  post: boolean
}

export const merchantCreateFormMatrix: readonly MerchantCreateFormRow[] = [
  { id: 'BVA-M360-040', referenceLength: 2, expectStatus: 400, layer: 'e2e', post: false },
  { id: 'BVA-M360-041', referenceLength: 3, expectStatus: 201, layer: 'e2e+rest', post: true },
  { id: 'BVA-M360-042', referenceLength: 64, expectStatus: 201, layer: 'e2e+rest', post: true },
  { id: 'BVA-M360-043', referenceLength: 65, expectStatus: 400, layer: 'e2e+rest', post: false },
  { id: 'EP-M360-045', expectStatus: 409, layer: 'e2e+rest', post: true },
]
