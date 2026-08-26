import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class VisualLabPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/visual-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('visual-tile-merchant-badge')).toBeVisible()
  }

  tile(testId: string) {
    return this.byTestId(testId)
  }

  async toggleBreak(): Promise<void> {
    await this.byTestId('visual-lab-break').click()
  }
}
