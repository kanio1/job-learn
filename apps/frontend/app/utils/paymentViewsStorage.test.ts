import { describe, expect, it } from 'vitest'
import type { PaymentView } from '~~/shared/types/payment-view'
import {
  PAYMENT_VIEWS_QUOTA,
  pickPaymentViewFilters,
  upsertLocalPaymentView,
} from './paymentViewsStorage'

function view(index: number, createdAt: string, isDefault = false): PaymentView {
  return {
    id: `view-${index}`,
    name: `View ${index}`,
    resource: 'PAYMENT_ORDERS',
    filters: { status: 'CAPTURED', currency: 'EUR', minAmount: 10000, sort: 'createdAt,desc' },
    columns: ['status'],
    isDefault,
    createdAt,
    updatedAt: createdAt,
  }
}

describe('pickPaymentViewFilters', () => {
  it('keeps the payment-list whitelist and drops page/size', () => {
    const filters = pickPaymentViewFilters({
      status: 'CAPTURED',
      currency: 'EUR',
      minAmount: 10000,
      maxAmount: 20000,
      fromDate: '2026-08-01',
      toDate: '2026-08-21',
      clientOrderReference: 'PO-1',
      sort: 'createdAt,desc',
      page: 3,
      size: 50,
      unknown: 'nope',
    })
    expect(filters).toEqual({
      status: 'CAPTURED',
      currency: 'EUR',
      minAmount: 10000,
      maxAmount: 20000,
      fromDate: '2026-08-01',
      toDate: '2026-08-21',
      clientOrderReference: 'PO-1',
      sort: 'createdAt,desc',
    })
    expect(filters).not.toHaveProperty('page')
    expect(filters).not.toHaveProperty('size')
  })
})

describe('upsertLocalPaymentView', () => {
  it('overwrites the oldest view when quota is exceeded', () => {
    const existing = Array.from({ length: PAYMENT_VIEWS_QUOTA }, (_, index) =>
      view(index, `2026-08-01T00:00:${String(index).padStart(2, '0')}Z`))
    const incoming = view(99, '2026-08-21T12:00:00Z')
    const next = upsertLocalPaymentView(existing, incoming)
    expect(next).toHaveLength(PAYMENT_VIEWS_QUOTA)
    expect(next.map(item => item.id)).not.toContain('view-0')
    expect(next.map(item => item.id)).toContain('view-99')
  })

  it('flips isDefault so only the incoming view stays default', () => {
    const next = upsertLocalPaymentView(
      [view(1, '2026-08-01T00:00:00Z', true)],
      view(2, '2026-08-02T00:00:00Z', true),
    )
    expect(next.find(item => item.id === 'view-1')?.isDefault).toBe(false)
    expect(next.find(item => item.id === 'view-2')?.isDefault).toBe(true)
  })
})
