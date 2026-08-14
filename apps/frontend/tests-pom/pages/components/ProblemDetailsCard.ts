import { expect, type Page } from '@playwright/test'

export class ProblemDetailsCard {
  constructor(private readonly page: Page) {}

  root() {
    return this.page.getByTestId('problem-details-card')
  }

  async expectVisible(): Promise<void> {
    await expect(this.root()).toBeVisible()
  }

  async expectStatusBadge(status: number | string): Promise<void> {
    await expect(this.root().getByText(new RegExp(`^${status}\\b`))).toBeVisible()
  }

  async expectError(code: string): Promise<void> {
    await expect(this.root().getByTestId('problem-error')).toHaveText(code)
  }
}
