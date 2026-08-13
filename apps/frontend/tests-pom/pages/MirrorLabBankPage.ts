import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class MirrorLabBankPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/mirror-lab/bank')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('step-up-submit')).toBeVisible()
  }
}
