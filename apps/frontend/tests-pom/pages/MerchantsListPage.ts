import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class MerchantsListPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/merchants')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('action-create-merchant')).toBeVisible()
  }

  async openCreateForm(): Promise<void> {
    await this.byTestId('action-create-merchant').click()
    await expect(this.byTestId('create-merchant-form')).toBeVisible()
  }

  async fillCreateForm(reference: string, displayName: string): Promise<void> {
    await this.page.getByLabel('Merchant reference').fill(reference)
    await this.page.getByLabel('Display name').fill(displayName)
  }

  async submitCreate(): Promise<void> {
    await this.byTestId('create-merchant-form').getByRole('button', { name: 'Create merchant' }).click()
  }

  async expectCreateFieldError(message: string | RegExp): Promise<void> {
    await expect(this.byTestId('create-merchant-form').getByText(message)).toBeVisible()
  }

  async expectRiskBadgeFor(displayName: string): Promise<void> {
    const row = this.page.getByRole('row').filter({ hasText: displayName })
    await expect(row.getByTestId('merchant-risk-badge')).toBeVisible()
  }

  async expectRowVisible(text: string): Promise<void> {
    await expect(this.page.getByRole('table').getByText(text)).toBeVisible()
  }

  async openMerchantByDisplayName(displayName: string): Promise<void> {
    await this.page.getByRole('link', { name: displayName }).click()
  }
}
