import type { OpsFeedFrame } from '~/schemas/ops-feed.schema'

export function mergeOpsFeedEvents(existing: OpsFeedFrame[], incoming: OpsFeedFrame[]): OpsFeedFrame[] {
  const byId = new Map<string, OpsFeedFrame>()
  for (const event of existing) {
    byId.set(event.eventId, event)
  }
  for (const event of incoming) {
    byId.set(event.eventId, event)
  }
  return [...byId.values()].sort((left, right) => {
    const leftTime = Date.parse(left.occurredAt)
    const rightTime = Date.parse(right.occurredAt)
    if (leftTime === rightTime) {
      return left.eventId.localeCompare(right.eventId)
    }
    return leftTime - rightTime
  })
}

export function opsFeedSocketUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/api/ops/feed`
}

/** Nitro closes an unauthenticated same-origin handshake with this code. */
export const OPS_FEED_UNAUTHORIZED_CLOSE = 4401

export function shouldReconnectOpsFeed(closeCode: number): boolean {
  return closeCode !== OPS_FEED_UNAUTHORIZED_CLOSE
}
