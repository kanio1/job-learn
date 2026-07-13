import { describe, expect, it } from 'vitest'
import { paymentOrderResponseSchema } from './payment-order.schema'

const seededPaymentOrder = {
  paymentOrderId: '00000000-0000-0000-0000-0000000000c1',
  merchantId: '00000000-0000-0000-0000-0000000000b1',
  clientOrderReference: 'SEED-ALPHA-001-CREATED',
  amountMinor: 1100,
  currency: 'PLN',
  status: 'CREATED',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('paymentOrderResponseSchema', () => {
  it('accepts the deterministic Java UUID format used by seeded payment data', () => {
    expect(paymentOrderResponseSchema.safeParse(seededPaymentOrder).success).toBe(true)
  })

  it('rejects malformed payment identifiers', () => {
    expect(paymentOrderResponseSchema.safeParse({ ...seededPaymentOrder, paymentOrderId: 'not-a-uuid' }).success)
      .toBe(false)
  })
})
