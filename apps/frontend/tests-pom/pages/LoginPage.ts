import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class LoginPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/login')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('login-control')).toBeVisible()
  }

  async continueToKeycloak(): Promise<void> {
    await this.byTestId('login-control').click()
  }
}
