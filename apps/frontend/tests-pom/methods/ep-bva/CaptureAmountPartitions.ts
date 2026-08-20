/**
 * EP/BVA — capture amountMinor vs authorized amount.
 *
 * What changes: capture payload size. Status stays AUTHORIZED until a legal capture.
 * Layer: REST (over-amount 422). UI covers full capture separately.
 * Seed: unique AUTHORIZED order. Never seed-learning.
 */

export const captureAmountPartitions = {
  id: 'SCN-CAP-OVER',
  authorizedMinor: 2100,
  overAmountMinor: 2101,
  expectStatus: 422,
  error: 'capture_amount_exceeds_authorized',
} as const
