import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class MerchantsListPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/merchants')
  }

  async expectLoaded(): Promise<void> {
    await this.expectHeading('Merchants')
  }

  /** SCN-ISO-09: merchants:read is false — alert, no registry table. */
  async expectAccessDenied(): Promise<void> {
    await expect(this.page.getByRole('alert').filter({
      hasText: 'You do not have permission to view merchants',
    })).toBeVisible()
    await expect(this.page.getByRole('table')).toHaveCount(0)
  }

  /** SCN-ISO-01/06: table is the registry, not the create button (tenant.admin also has create). */
  async expectRegistryTable(): Promise<void> {
    await expect(this.page.getByRole('table')).toBeVisible()
  }

  async expectRowAbsent(text: string): Promise<void> {
    await expect(this.page.getByRole('table').getByText(text, { exact: true })).toHaveCount(0)
  }

  async openCreateForm(): Promise<void> {
    await this.byTestId('action-create-merchant').click()
    await expect(this.byTestId('create-merchant-form')).toBeVisible()
  }

  async fillCreateForm(reference: string, displayName: string, tenantReference?: string): Promise<void> {
    await this.page.getByLabel('Merchant reference').fill(reference)
    await this.page.getByLabel('Display name').fill(displayName)
    if (tenantReference) {
      await this.page.getByLabel('Tenant reference').fill(tenantReference)
    }
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

  async filterByText(text: string): Promise<void> {
    await this.page.getByPlaceholder('Filter merchants...').fill(text)
  }

  async filterByStatus(label: string): Promise<void> {
    await this.page.getByLabel('Filter status').click()
    await this.page.getByRole('option', { name: label }).click()
  }

  async expectRowVisible(text: string): Promise<void> {
    await expect(this.page.getByRole('table').getByRole('cell', { name: text, exact: true })).toBeVisible()
  }

  async openMerchantByDisplayName(displayName: string): Promise<void> {
    await this.page.getByRole('link', { name: displayName }).click()
  }
}
