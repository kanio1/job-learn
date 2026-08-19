import { expect, type Locator, type Page } from '@playwright/test'

export class CommandPalette {
  constructor(private readonly page: Page) {}

  dialog(): Locator {
    return this.page.getByRole('dialog')
  }

  async openWithKeyboard(): Promise<void> {
    await this.page.keyboard.press('Control+k')
    await expect(this.page.getByText('Search Payment Quality Lab')).toBeVisible()
  }

  async search(text: string): Promise<void> {
    await this.page.keyboard.type(text)
  }

  async selectOption(name: string): Promise<void> {
    await this.dialog().getByRole('option', { name, exact: true }).first().click()
  }
}
