import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class CheckoutReturnPage extends BasePage {
  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('checkout-return')).toBeVisible()
  }

  fulfillmentStatus() {
    return this.byTestId('fulfillment-status')
  }

  returnHint() {
    return this.byTestId('return-hint')
  }
}
