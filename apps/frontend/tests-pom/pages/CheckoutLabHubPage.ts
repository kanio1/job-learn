import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class CheckoutLabHubPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/checkout-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByText('Three identity worlds').or(this.page.getByText('Hosted capability'))).toBeVisible()
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
