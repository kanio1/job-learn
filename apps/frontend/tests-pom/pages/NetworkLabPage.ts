import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class NetworkLabPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/network-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('network-lab-trigger-503')).toBeVisible()
  }

  async trigger503(): Promise<void> {
    await this.byTestId('network-lab-trigger-503').click()
  }

  async triggerLie(): Promise<void> {
    await this.byTestId('network-lab-lie').click()
  }

  result(): Locator {
    return this.byTestId('network-lab-result')
  }

  errorState(): Locator {
    return this.byTestId('error-state')
  }
}
