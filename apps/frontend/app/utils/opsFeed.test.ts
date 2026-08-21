import { describe, expect, it } from 'vitest'
import type { OpsFeedFrame } from '~/schemas/ops-feed.schema'
import { mergeOpsFeedEvents, shouldReconnectOpsFeed } from './opsFeed'

function frame(eventId: string, occurredAt: string, label: string): OpsFeedFrame {
  return {
    eventId,
    occurredAt,
    merchantId: '11111111-1111-1111-1111-111111111111',
    paymentOrderId: '22222222-2222-2222-2222-222222222222',
    type: 'PAYMENT_CAPTURED',
    label,
  }
}

describe('mergeOpsFeedEvents', () => {
  it('keeps one row per eventId and sorts by occurredAt', () => {
    const first = frame('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-08-20T10:42:03Z', 'PO-T2')
    const earlier = frame('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '2026-08-20T10:41:01Z', 'PO-T1')
    const duplicate = { ...first, label: 'PO-T2-dup' }
    const merged = mergeOpsFeedEvents([first], [earlier, duplicate])
    expect(merged).toHaveLength(2)
    expect(merged[0]?.eventId).toBe(earlier.eventId)
    expect(merged[1]?.eventId).toBe(first.eventId)
    expect(merged[1]?.label).toBe('PO-T2-dup')
  })
})

describe('shouldReconnectOpsFeed', () => {
  it('does not reconnect after a 4401 unauthorized handshake', () => {
    expect(shouldReconnectOpsFeed(4401)).toBe(false)
    expect(shouldReconnectOpsFeed(1000)).toBe(true)
    expect(shouldReconnectOpsFeed(1006)).toBe(true)
  })
})
