import { expect, type Locator, type Page } from '@playwright/test'

export class ConfirmModal {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('confirm-action-modal')
  }

  heading(title: string | RegExp): Locator { return this.page.getByRole('heading', { name: title }) }

  async expectOpen(title?: string | RegExp): Promise<void> {
    if (title) {
      await expect(this.page.getByRole('heading', { name: title })).toBeVisible()
    } else {
      await expect(this.page.getByRole('button', { name: 'Confirm' })).toBeVisible()
    }
  }

  async confirm(): Promise<void> {
    await this.page.getByTestId('confirm-action-confirm').click()
  }

  /**
   * Dismiss without confirming. Uses the footer test id so this never matches
   * a lifecycle drawer button labelled "Cancel".
   */
  async dismiss(): Promise<void> {
    await this.page.getByTestId('confirm-action-dismiss').click()
  }
}
