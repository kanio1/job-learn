import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class TenantSettingsPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/tenant/settings')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('tenant-settings-form')).toBeVisible()
  }

  async fillContactEmail(email: string): Promise<void> {
    await this.byTestId('tenant-settings-contact-email').fill(email)
  }

  async save(): Promise<void> {
    await this.byTestId('tenant-settings-save').click()
  }
}
