import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class CheckoutLabBookingPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/checkout-lab/booking')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('checkout-booking-form')).toBeVisible()
    await expect(this.byTestId('checkout-booking-submit')).toBeVisible()
  }

  async fillExtOrderId(value: string): Promise<void> {
    await this.byTestId('checkout-booking-ext-order').fill(value)
  }

  async chooseMode(mode: 'ONLINE' | 'CASH'): Promise<void> {
    await this.byTestId('checkout-booking-mode').click()
    await this.page.getByRole('option', { name: mode }).click()
  }

  async submit(): Promise<void> {
    await this.byTestId('checkout-booking-submit').click()
  }

  async hostedCheckoutHref(): Promise<string | null> {
    return this.byTestId('checkout-open-hosted').getAttribute('href')
  }

  async openHostedCheckout(): Promise<void> {
    await this.byTestId('checkout-open-hosted').click()
  }
}
