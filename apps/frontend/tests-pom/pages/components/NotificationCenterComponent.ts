import { expect, type Locator, type Page } from '@playwright/test'

export class NotificationCenterComponent {
  constructor(private readonly page: Page) {}

  bell(): Locator {
    return this.page.getByRole('button', { name: 'Notifications' })
  }

  popover(): Locator {
    return this.page.getByTestId('notification-popover')
  }

  unreadBadge(): Locator {
    return this.page.getByTestId('notification-unread-count')
  }

  item(notificationId: string): Locator {
    return this.page.getByTestId(`notification-item-${notificationId}`)
  }

  async open(): Promise<void> {
    await this.bell().click()
    await expect(this.popover()).toBeVisible()
  }

  markReadButton(notificationId: string): Locator {
    return this.item(notificationId).getByRole('button', { name: 'Mark as read' })
  }

  readAllButton(): Locator {
    return this.popover().getByRole('button', { name: 'Read all' })
  }

  async markRead(notificationId: string): Promise<void> {
    const button = this.markReadButton(notificationId)
    await expect(button).toBeInViewport()
    await button.click()
  }

}
