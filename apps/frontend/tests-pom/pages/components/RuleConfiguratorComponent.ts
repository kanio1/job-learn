import { expect, type Locator, type Page } from '@playwright/test'

export class RuleConfiguratorComponent {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('rule-configurator')
  }

  autoCaptureSwitch(): Locator {
    return this.page.getByRole('switch', { name: 'Auto capture' })
  }

  maxAmount(): Locator {
    return this.page.getByTestId('policy-max-auto-capture')
  }

  maxAmountInput(): Locator {
    return this.maxAmount().locator('input')
  }

  riskSlider(): Locator {
    return this.page.getByTestId('policy-risk-threshold').getByRole('slider')
  }

  refundGroup(): Locator {
    return this.page.getByTestId('policy-refund-policy')
  }

  refundRadio(label: 'Manual' | 'Automatic'): Locator {
    return this.page.getByRole('radio', { name: label })
  }

  maxError(): Locator {
    return this.page.getByTestId('policy-max-error')
  }

  async expectOpen(): Promise<void> {
    await expect(this.root()).toBeVisible()
    await expect(this.autoCaptureSwitch()).toBeVisible()
    await expect(this.riskSlider()).toBeVisible()
  }

  async setAutoCapture(enabled: boolean): Promise<void> {
    const current = await this.autoCaptureSwitch().getAttribute('aria-checked')
    const isOn = current === 'true'
    if (isOn !== enabled) {
      await this.autoCaptureSwitch().click()
    }
  }

  async clearMaxAmount(): Promise<void> {
    await this.maxAmountInput().fill('')
  }

  async fillMaxAmount(value: number): Promise<void> {
    await this.maxAmountInput().fill(String(value))
  }

  async expectSlider(value: number): Promise<void> {
    await expect(this.riskSlider()).toHaveAttribute('aria-valuenow', String(value))
    await expect(this.riskSlider()).toHaveAttribute('aria-valuemin', '0')
    await expect(this.riskSlider()).toHaveAttribute('aria-valuemax', '100')
  }
}
