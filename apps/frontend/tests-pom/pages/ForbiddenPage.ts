import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class ForbiddenPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/forbidden')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.root()).toBeVisible()
    await expect(this.heading()).toBeVisible()
  }

  root(): Locator { return this.byTestId('forbidden-page') }
  heading(): Locator { return this.page.getByRole('heading', { name: 'Access Denied' }) }
}
