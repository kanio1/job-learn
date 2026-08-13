import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class SessionLabPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/session-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('session-lab-js-cookies')).toBeVisible()
  }
}
