/**
 * UC-W2-08 — manager creates an operator payment order on ALPHA_001.
 *
 * What changes vs CPL: no continueUrl, no hosted tab. Oracle = CREATED + GET persist.
 * Layer: E2E form + REST persist. EP rows live in AmountPartitions (REST).
 * Seed: unique clientOrderReference + Idempotency-Key. Never seed-learning.
 */

export const createOrderJourney = {
  id: 'SCN-PAY-01',
  merchantId: '00000000-0000-0000-0000-0000000000b1',
  expectStatus: 201,
  expectEtag: '"v0"',
  expectLifecycle: 'CREATED',
} as const
