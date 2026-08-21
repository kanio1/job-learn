import { expect, type Locator, type Page } from '@playwright/test'

export class LocaleSelectComponent {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('locale-select')
  }

  sampleAmount(): Locator {
    return this.page.getByTestId('locale-sample-amount')
  }

  sampleDate(): Locator {
    return this.page.getByTestId('locale-sample-date')
  }

  async expectOpen(): Promise<void> {
    await expect(this.root()).toBeVisible()
  }

  async select(name: 'English' | 'Polski' | 'Svenska'): Promise<void> {
    await this.root().click()
    await this.page.getByRole('option', { name }).click()
  }
}
