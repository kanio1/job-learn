import { describe, it, expect } from 'vitest'
import { eventLabRecordSchema } from './event-lab.schema'

const valid = {
  id: '11111111-1111-4111-8111-111111111111',
  eventId: '22222222-2222-4222-8222-222222222222',
  consumerGroup: 'eventlab-inspector',
  action: 'PAYMENT_AUTHORIZED',
  targetType: 'PAYMENT_ORDER',
  targetId: '33333333-3333-4333-8333-333333333333',
  tenantRef: 'TENANT_ALPHA',
  status: 'PROCESSED' as const,
  attempts: 1,
  consumedAt: '2026-08-23T10:00:00Z',
  topic: 'lab.auditable-actions.v1',
  partitionNo: 0,
  recordOffset: 0,
  recordKey: '44444444-4444-4444-8444-444444444444',
}

describe('VT-KAFKA-001 valid envelope parses', () => {
  it('parses', () => {
    expect(eventLabRecordSchema.safeParse(valid).success).toBe(true)
  })
})
describe('VT-KAFKA-002 missing eventId fails', () => {
  it('fails without eventId', () => {
    const { eventId: _omit, ...rest } = valid
    expect(eventLabRecordSchema.safeParse(rest).success).toBe(false)
  })
  it('fails with bad status', () => {
    expect(eventLabRecordSchema.safeParse({ ...valid, status: 'UNKNOWN' }).success).toBe(false)
  })
})
describe('VT-KAFKA-003 excess fields stripped', () => {
  it('strips', () => {
    const withExtra = { ...valid, extra: 'x', foo: 1 }
    const res = eventLabRecordSchema.safeParse(withExtra)
    expect(res.success).toBe(true)
    if (res.success) expect((res.data as Record<string, unknown>).extra).toBeUndefined()
  })
})
