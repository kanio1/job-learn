import { expect, type Locator, type Page, type WebSocket } from '@playwright/test'

export class OpsFeedComponent {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('ops-feed')
  }

  chip(): Locator {
    return this.page.getByTestId('ops-feed-chip')
  }

  timeline(): Locator {
    return this.page.getByTestId('ops-feed-timeline')
  }

  rowByEventId(eventId: string): Locator {
    return this.page.getByTestId(`ops-feed-row-${eventId}`)
  }

  rows(): Locator {
    return this.timeline().locator('li')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.root()).toBeVisible()
    await expect(this.chip()).toBeVisible()
  }

  async expectConnected(): Promise<void> {
    await expect(this.chip()).toHaveText('connected')
  }

  async expectDisconnected(): Promise<void> {
    await expect(this.chip()).toHaveText('disconnected')
  }

  async waitForOpsEvent(match: { eventId?: string, type?: string, orderRef?: string }): Promise<void> {
    if (match.eventId) {
      await expect(this.rowByEventId(match.eventId)).toBeVisible()
      return
    }
    const parts = [match.orderRef, match.type].filter(Boolean)
    if (parts.length === 0) {
      throw new Error('waitForOpsEvent requires eventId, type, or orderRef')
    }
    const needle = parts.join('  ')
    await expect(this.page.getByTestId('ops-feed-label').filter({ hasText: needle }).first()).toBeVisible()
  }

  attachWebSocket(): Promise<WebSocket> {
    return this.page.waitForEvent('websocket', ws => ws.url().includes('/api/ops/feed'))
  }
}
