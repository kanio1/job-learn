import { expect, type Page } from '@playwright/test'
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

  async expectExpired(): Promise<void> {
    await expect(this.byTestId('psp-link-expired')).toBeVisible()
  }

  async expectOutcome(): Promise<void> {
    await expect(this.byTestId('psp-outcome')).toBeVisible()
  }

  async returnToMerchant(): Promise<void> {
    await this.page.getByRole('link', { name: /return to merchant/i })
      .or(this.page.getByRole('button', { name: /return to merchant/i }))
      .click()
  }
}
