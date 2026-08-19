import { expect, type Page } from '@playwright/test'
import { BasePage } from './BasePage'

export class PspRedirectSimulatorPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('psp-redirect-simulator')).toBeVisible()
    await expect(this.byTestId('psp-approve')).toBeVisible()
  }

  async approve(): Promise<void> {
    await this.assertNoDevErrorOverlay()
    await expect(async () => {
      await this.byTestId('psp-approve').click()
      await expect(this.byTestId('psp-outcome')).toContainText('Payment approved', { timeout: 1_000 })
    }).toPass({ timeout: 15_000 })
  }
}
