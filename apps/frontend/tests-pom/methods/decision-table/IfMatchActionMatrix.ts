/**
 * DT — lifecycle action × If-Match shape (not authorize-only).
 *
 * What changes: verb + header on/off. Status stays the legal `from`.
 * Layer: REST; 4 lifecycle rows + 2 PATCH rows, not a cartesian E2E.
 * Seed: unique CREATED/AUTHORIZED order on ALPHA_001. Never seed-learning.
 */

import type { PaymentStatus } from '../state/PaymentStatusMachine'

export type IfMatchActionRow = {
  id: string
  from: PaymentStatus
  action: 'cancel' | 'capture' | 'patch'
  ifMatch: 'absent' | 'v99' | 'malformed'
  expectStatus: 400 | 412 | 428
  to: PaymentStatus
}

export const ifMatchActionMatrix = [
  { id: 'SCN-IFM-01', from: 'CREATED', action: 'cancel', ifMatch: 'absent', expectStatus: 428, to: 'CREATED' },
  { id: 'SCN-IFM-02', from: 'AUTHORIZED', action: 'capture', ifMatch: 'v99', expectStatus: 412, to: 'AUTHORIZED' },
  { id: 'SCN-IFM-03', from: 'CREATED', action: 'cancel', ifMatch: 'malformed', expectStatus: 400, to: 'CREATED' },
  { id: 'SCN-IFM-04', from: 'AUTHORIZED', action: 'capture', ifMatch: 'absent', expectStatus: 428, to: 'AUTHORIZED' },
] as const satisfies readonly IfMatchActionRow[]

export const ifMatchPatchMatrix = [
  { id: 'SCN-IFM-05', from: 'CREATED', action: 'patch', ifMatch: 'v99', expectStatus: 412, to: 'CREATED' },
  { id: 'SCN-IFM-06', from: 'CREATED', action: 'patch', ifMatch: 'absent', expectStatus: 428, to: 'CREATED' },
] as const satisfies readonly IfMatchActionRow[]
