import { expect, type Locator, type Page } from '@playwright/test'

export class PinChallengeComponent {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('refund-pin-challenge')
  }

  input(): Locator {
    return this.page.getByTestId('refund-pin-input')
  }

  verifyButton(): Locator {
    return this.page.getByTestId('refund-pin-verify')
  }

  error(): Locator {
    return this.page.getByTestId('refund-pin-error')
  }

  lockedAlert(): Locator {
    return this.page.getByTestId('refund-pin-locked')
  }

  pinLabel(): Locator {
    return this.page.getByLabel('Refund approval PIN')
  }

  async expectOpen(): Promise<void> {
    await expect(this.root()).toBeVisible()
    await expect(this.pinLabel()).toBeVisible()
  }

  async typePin(pin: string): Promise<void> {
    // Six visual slots implement one PIN field; slot zero is the intentional keyboard entry point.
    const slots = this.input().locator('input')
    await slots.first().click()
    await this.page.keyboard.type(pin)
  }

  async pastePin(pin: string): Promise<void> {
    // Six visual slots implement one PIN field; slot zero is the intentional paste target.
    const slots = this.input().locator('input')
    await slots.first().fill(pin)
  }

  async submit(): Promise<void> {
    await this.verifyButton().click()
  }
}
