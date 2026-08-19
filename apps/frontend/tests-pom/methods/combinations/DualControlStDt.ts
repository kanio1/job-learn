/**
 * Combination ST (only CAPTURED) + DT (maker sub ≠ checker sub).
 *
 * Why together: “refund works” hides dual-control. Direct refund 409 is expected.
 * Self-approve 409. Other subject + If-Match → REFUNDED.
 * Layer: existing payments-refund-dual-control.spec.ts. Never seed-learning.
 */

export const dualControlSteps = [
  { id: 'SCN-DC-01', actor: 'maker', action: 'POST /refund', expectStatus: 409, error: 'dual_control_required' },
  { id: 'SCN-DC-02', actor: 'maker', action: 'POST /refund-approvals', expectStatus: 201 },
  { id: 'SCN-DC-03', actor: 'maker', action: 'POST …/approve', expectStatus: 409, error: 'dual_control_self_approve' },
  { id: 'SCN-DC-04', actor: 'checker', action: 'POST …/approve', expectStatus: 200, to: 'REFUNDED' },
] as const
