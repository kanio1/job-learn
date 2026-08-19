import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class NetworkLabPage extends BasePage {
  async goto(): Promise<void> {
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

  result() {
    return this.byTestId('network-lab-result')
  }

  errorState() {
    return this.byTestId('error-state')
  }
}
