import { expect, type Locator, type Page } from '@playwright/test'

export class ProblemDetailsCard {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('problem-details-card')
  }

  async expectVisible(): Promise<void> {
    await expect(this.root()).toBeVisible()
  }

  statusBadge(status: number | string): Locator {
    return this.root().getByText(new RegExp(`^${status}\\b`))
  }

  errorCode(): Locator {
    return this.root().getByTestId('problem-error')
  }
}
