import { expect, type Locator, type Page } from '@playwright/test'

export class ConflictDiffComponent {
  constructor(private readonly page: Page) {}

  dialog(): Locator {
    return this.page.getByTestId('merchant-conflict-dialog')
  }

  async expectOpen(): Promise<void> {
    await expect(this.dialog()).toBeVisible()
    await expect(this.page.getByRole('tab', { name: /your changes/i })).toBeVisible()
    await expect(this.page.getByRole('tab', { name: /latest version/i })).toBeVisible()
  }

  async discardMine(): Promise<void> {
    await this.page.getByTestId('conflict-discard').click()
    await expect(this.dialog()).toBeHidden()
  }

  async reloadLatest(): Promise<void> {
    await this.page.getByTestId('conflict-reload').click()
    await expect(this.dialog()).toBeHidden()
  }
}
