/**
 * ST — operator payment_orders (not CPL fulfillment).
 *
 * Legal: CREATED→AUTHORIZED→CAPTURED; CREATED→CANCELLED.
 * Guarded: stale If-Match 412 stays CREATED; missing If-Match 428;
 * cancel from CAPTURED 422; merchant refund 409 dual_control_required.
 * Layer: REST owns illegal edges; E2E owns P0 edges + ConfirmModal.
 * Seed: unique order on ALPHA_001. Never seed-learning.
 */

export type PaymentStatus = 'CREATED' | 'AUTHORIZED' | 'CAPTURED' | 'CANCELLED' | 'REFUNDED'

export type PaymentEdge = {
  id: string
  from: PaymentStatus
  action: 'authorize' | 'capture' | 'cancel' | 'refund'
  ifMatch: 'fresh' | 'v99' | 'absent' | 'malformed'
  expectStatus: number
  to: PaymentStatus
}

export const paymentStatusEdges: readonly PaymentEdge[] = [
  { id: 'SCN-LIF-01', from: 'CREATED', action: 'authorize', ifMatch: 'fresh', expectStatus: 200, to: 'AUTHORIZED' },
  { id: 'SCN-LIF-02', from: 'AUTHORIZED', action: 'capture', ifMatch: 'fresh', expectStatus: 200, to: 'CAPTURED' },
  { id: 'SCN-LIF-03', from: 'CREATED', action: 'authorize', ifMatch: 'v99', expectStatus: 412, to: 'CREATED' },
  { id: 'SCN-LIF-04', from: 'CREATED', action: 'authorize', ifMatch: 'absent', expectStatus: 428, to: 'CREATED' },
  { id: 'SCN-LIF-05', from: 'CREATED', action: 'authorize', ifMatch: 'malformed', expectStatus: 400, to: 'CREATED' },
  { id: 'SCN-CAN-01', from: 'CREATED', action: 'cancel', ifMatch: 'fresh', expectStatus: 200, to: 'CANCELLED' },
  { id: 'SCN-ILL-01', from: 'CREATED', action: 'capture', ifMatch: 'fresh', expectStatus: 422, to: 'CREATED' },
  { id: 'SCN-ILL-02', from: 'CREATED', action: 'refund', ifMatch: 'fresh', expectStatus: 409, to: 'CREATED' },
  { id: 'SCN-ILL-03', from: 'AUTHORIZED', action: 'authorize', ifMatch: 'fresh', expectStatus: 422, to: 'AUTHORIZED' },
  { id: 'SCN-ILL-04', from: 'CAPTURED', action: 'capture', ifMatch: 'fresh', expectStatus: 422, to: 'CAPTURED' },
  { id: 'SCN-ILL-05', from: 'CANCELLED', action: 'authorize', ifMatch: 'fresh', expectStatus: 422, to: 'CANCELLED' },
]
