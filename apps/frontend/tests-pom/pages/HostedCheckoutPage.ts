import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'

export class HostedCheckoutPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('psp-hosted-checkout')).toBeVisible()
  }

  async approve(): Promise<void> {
    await this.byTestId('psp-approve').click()
  }

  async decline(): Promise<void> {
    await this.byTestId('psp-decline').click()
  }

  expiredNotice(): Locator {
    return this.byTestId('psp-link-expired')
  }

  outcome(): Locator {
    return this.byTestId('psp-outcome')
  }

  async returnToMerchant(): Promise<void> {
    await this.page.getByRole('link', { name: /return to merchant/i })
      .or(this.page.getByRole('button', { name: /return to merchant/i }))
      .click()
  }
}
