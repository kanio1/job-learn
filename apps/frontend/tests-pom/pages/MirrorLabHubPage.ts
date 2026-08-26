import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class MirrorLabHubPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/mirror-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByText('Three identity worlds')).toBeVisible()
  }

  async openSession(): Promise<void> {
    await this.byTestId('mirror-lab-open-session').click()
  }

  async openNetwork(): Promise<void> {
    await this.byTestId('mirror-lab-open-network').click()
  }

  async openBank(): Promise<void> {
    await this.byTestId('mirror-lab-open-bank').click()
  }
}
