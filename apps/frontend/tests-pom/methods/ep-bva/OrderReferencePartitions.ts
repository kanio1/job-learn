/**
 * EP/BVA — clientOrderReference on create payment-order.
 *
 * What changes: length only (blank / 1 / 120 / 121). Same ALPHA_001 + unique key.
 * Layer: REST. Zod and `ClientOrderReference` share 1..120.
 * Seed: unique factory. Never seed-learning.
 */

export type OrderReferencePartition = {
  id: string
  kind: 'blank' | 'min' | 'max' | 'over'
  expectStatus: 201 | 400
}

export const MAX_ORDER_REFERENCE_LENGTH = 120

export const orderReferencePartitions: readonly OrderReferencePartition[] = [
  { id: 'SCN-PAY-12', kind: 'blank', expectStatus: 400 },
  { id: 'SCN-PAY-13', kind: 'min', expectStatus: 201 },
  { id: 'SCN-PAY-14', kind: 'max', expectStatus: 201 },
  { id: 'SCN-PAY-15', kind: 'over', expectStatus: 400 },
]

export function orderReferenceFor(kind: OrderReferencePartition['kind'], uniqueSeed: string): string {
  const seed = uniqueSeed.replace(/[^A-Za-z0-9]/g, '').toUpperCase() || 'X'
  if (kind === 'blank') {
    return '   '
  }
  if (kind === 'min') {
    return seed.slice(0, 1)
  }
  if (kind === 'max') {
    return (seed + 'X'.repeat(MAX_ORDER_REFERENCE_LENGTH)).slice(0, MAX_ORDER_REFERENCE_LENGTH)
  }
  return 'X'.repeat(MAX_ORDER_REFERENCE_LENGTH + 1)
}
