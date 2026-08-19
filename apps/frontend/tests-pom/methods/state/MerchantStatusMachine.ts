/**
 * ST — merchant DRAFT → ACTIVE → SUSPENDED.
 *
 * What changes: current status. Illegal reactivate of SUSPENDED → 409.
 * Layer: E2E two legal edges (E2E-021); illegal edge REST/RA.
 * Seed: unique merchant via BffClient (tenantReference TENANT_ALPHA).
 */

export const merchantLegalPath = ['DRAFT', 'ACTIVE', 'SUSPENDED'] as const

export const merchantIllegalReactivate = {
  id: 'SCN-MER-04',
  from: 'SUSPENDED',
  action: 'activate',
  expectStatus: 409,
  error: 'invalid_transition',
} as const
