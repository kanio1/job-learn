import { expect, type Locator, type Page } from '@playwright/test'

export abstract class BasePage {
  constructor(protected readonly page: Page) {}

  protected byTestId(testId: string): Locator {
    return this.page.getByTestId(testId)
  }

  async goto(path: string): Promise<void> {
    await this.page.goto(path)
  }

  abstract expectLoaded(): Promise<void>

  async expectHeading(name: string | RegExp): Promise<void> {
    await expect(this.page.getByRole('heading', { name })).toBeVisible()
  }
}
