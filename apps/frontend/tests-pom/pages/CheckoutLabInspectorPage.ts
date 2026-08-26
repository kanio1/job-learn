import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class CheckoutLabInspectorPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/checkout-lab/inspector')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('inspector-load')).toBeVisible()
  }

  async loadSession(sessionId: string): Promise<void> {
    await this.byTestId('inspector-session-id').fill(sessionId)
    await this.byTestId('inspector-load').click()
  }
}
