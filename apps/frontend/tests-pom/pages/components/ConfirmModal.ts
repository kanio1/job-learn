import { expect, type Page } from '@playwright/test'

export class ConfirmModal {
  constructor(private readonly page: Page) {}

  root() {
    return this.page.getByTestId('confirm-action-modal')
  }

  async expectOpen(title?: string | RegExp): Promise<void> {
    if (title) {
      await expect(this.page.getByRole('heading', { name: title })).toBeVisible()
    } else {
      await expect(this.page.getByRole('button', { name: 'Confirm' })).toBeVisible()
    }
  }

  async confirm(): Promise<void> {
    await this.page.getByRole('button', { name: 'Confirm' }).click()
  }

  async cancel(): Promise<void> {
    await this.page.getByRole('button', { name: 'Go back' }).or(this.page.getByRole('button', { name: 'Cancel' })).click()
  }
}
