import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class UsersPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/users')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('users-table')).toBeVisible()
  }

  async expectForbidden(): Promise<void> {
    await expect(this.byTestId('forbidden-state')).toBeVisible()
  }
}
