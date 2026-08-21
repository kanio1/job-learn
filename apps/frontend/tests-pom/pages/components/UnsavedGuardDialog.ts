import { expect, type Locator, type Page } from '@playwright/test'

export class UnsavedGuardDialog {
  constructor(private readonly page: Page) {}

  dialog(): Locator {
    return this.page.getByTestId('unsaved-changes-dialog')
  }

  async expectOpen(): Promise<void> {
    await expect(this.dialog()).toBeVisible()
  }

  async stay(): Promise<void> {
    await this.page.getByTestId('unsaved-stay').click()
    await expect(this.dialog()).toBeHidden()
  }

  async discard(): Promise<void> {
    await this.page.getByTestId('unsaved-discard').click()
  }
}
