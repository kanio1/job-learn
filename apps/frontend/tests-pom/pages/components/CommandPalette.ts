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

  async openFromButton(): Promise<void> {
    await this.page.getByRole('button', { name: /Search/ }).click()
    await expect(this.page.getByText('Search Payment Quality Lab')).toBeVisible()
  }

  searchInput(): Locator {
    return this.dialog().getByPlaceholder('Search dashboard...')
  }

  async search(text: string): Promise<void> {
    await this.page.keyboard.type(text)
  }

  async fillSearch(text: string): Promise<void> {
    await this.searchInput().fill(text)
  }

  option(name: string): Locator {
    return this.dialog().getByRole('option', { name })
  }

  optionInGroup(group: string, name: string): Locator {
    return this.dialog().getByLabel(group).getByRole('option', { name, exact: true })
  }

  async selectOptionInGroup(group: string, name: string): Promise<void> {
    await this.optionInGroup(group, name).click()
  }
}
