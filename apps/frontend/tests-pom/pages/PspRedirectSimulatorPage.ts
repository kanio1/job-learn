import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'

export class PspRedirectSimulatorPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('psp-redirect-simulator')).toBeVisible()
    await expect(this.byTestId('psp-approve')).toBeVisible()
  }

  outcome(): Locator {
    return this.byTestId('psp-outcome')
  }

  root(): Locator { return this.byTestId('psp-redirect-simulator') }
  approveButton(): Locator { return this.byTestId('psp-approve') }

  /** Approve once; the spec owns the visible business-outcome assertion. */
  async approve(): Promise<void> {
    await this.assertNoDevErrorOverlay()
    await expect(this.approveButton()).toBeVisible()
    await this.approveButton().click()
  }
}
