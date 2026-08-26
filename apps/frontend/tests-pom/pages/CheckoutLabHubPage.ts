import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class CheckoutLabHubPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/checkout-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('checkout-lab-open-booking')).toBeVisible()
  }

  async openBooking(): Promise<void> {
    await this.byTestId('checkout-lab-open-booking').click()
  }

  async openInspector(): Promise<void> {
    await this.byTestId('checkout-lab-open-inspector').click()
  }

  async openWidget(): Promise<void> {
    await this.byTestId('checkout-lab-open-widget').click()
  }
}
