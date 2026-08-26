import { expect, type FrameLocator } from '@playwright/test'
import { BasePage } from './BasePage'

export class CheckoutLabWidgetPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/checkout-lab/widget')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('widget-session-id')).toBeVisible()
    await expect(this.byTestId('widget-load')).toBeVisible()
  }

  async loadSession(sessionId: string): Promise<void> {
    await this.byTestId('widget-session-id').fill(sessionId)
    await this.byTestId('widget-load').click()
    await expect(this.byTestId('checkout-lab-widget-frame')).toBeVisible()
  }

  frame(): FrameLocator {
    return this.byTestId('checkout-lab-widget-frame').contentFrame()
  }

  async approveInFrame(): Promise<void> {
    await expect(this.frame().getByTestId('psp-hosted-checkout')).toBeVisible()
    await this.frame().getByTestId('psp-approve').click()
  }

  async expectApprovedInFrame(): Promise<void> {
    await expect(this.frame().getByTestId('psp-outcome')).toBeVisible()
    await expect(this.frame().getByTestId('psp-outcome')).toContainText(/approved/i)
  }
}
