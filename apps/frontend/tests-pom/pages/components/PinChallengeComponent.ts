import { expect, type Locator, type Page } from '@playwright/test'

export class PinChallengeComponent {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('refund-pin-challenge')
  }

  input(): Locator {
    return this.page.getByTestId('refund-pin-input')
  }

  async expectOpen(): Promise<void> {
    await expect(this.root()).toBeVisible()
    await expect(this.page.getByLabel('Refund approval PIN')).toBeVisible()
  }

  async typePin(pin: string): Promise<void> {
    const slots = this.input().locator('input')
    await slots.first().click()
    await this.page.keyboard.type(pin)
  }

  async pastePin(pin: string): Promise<void> {
    const slots = this.input().locator('input')
    await slots.first().fill(pin)
  }

  /** Submit the PIN when the verify button is enabled (PIN may need typing). */
  async submitIfEnabled(): Promise<void> {
    const verify = this.page.getByTestId('refund-pin-verify')
    if (await verify.isEnabled()) {
      await verify.click()
    }
  }
}
