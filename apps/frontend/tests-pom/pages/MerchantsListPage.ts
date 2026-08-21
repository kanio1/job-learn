import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'
import { waitForBffResponse } from '../utils/wait-bff'

export class MerchantsListPage extends BasePage {
  async goto(query = ''): Promise<void> {
    await super.goto(`/admin/merchants${query}`)
  }

  async expectLoaded(): Promise<void> {
    await this.expectHeading('Merchants')
    await expect(this.page.getByTestId('loading-state')).toHaveCount(0)
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
    await this.expectHeading('Merchants')
    await expect(this.page.getByTestId('loading-state')).toHaveCount(0)
    await expect(this.page.getByRole('table').filter({
      has: this.page.getByRole('columnheader', { name: 'Reference' }),
    })).toBeVisible()
  }

  rowByReference(reference: string): Locator {
    return this.page.getByRole('row').filter({ hasText: reference })
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
    await this.page.getByLabel('Filter merchants').fill(text)
  }

  async applyFilters(): Promise<void> {
    const listed = waitForBffResponse(this.page, { method: 'GET', pathExact: '/api/merchants' })
    await this.byTestId('merchant-filter-apply').click()
    await listed
  }

  async filterByStatus(label: string): Promise<void> {
    await this.page.getByLabel('Filter status').click()
    await this.page.getByRole('option', { name: label }).click()
    await this.applyFilters()
  }

  async filterByTenant(label: string): Promise<void> {
    await this.page.getByLabel('Filter tenant').click()
    await this.page.getByRole('option', { name: label, exact: true }).click()
    await this.applyFilters()
  }

  async selectRow(reference: string): Promise<void> {
    await this.page.getByRole('checkbox', { name: `Select ${reference}` }).check()
  }

  async bulkActivate(): Promise<void> {
    await this.byTestId('merchant-bulk-activate').click()
  }

  async sortBy(column: RegExp | string): Promise<void> {
    const name = typeof column === 'string' ? new RegExp(column, 'i') : column
    await this.page.getByRole('columnheader', { name }).click()
  }

  async expectRowVisible(text: string): Promise<void> {
    await expect(this.page.getByRole('table').getByRole('cell', { name: text, exact: true })).toBeVisible()
  }

  async openMerchantByDisplayName(displayName: string): Promise<void> {
    await this.page.getByRole('button', { name: displayName }).click()
  }

  async open360(reference: string): Promise<void> {
    await this.page.getByRole('button', { name: `Open ${reference}` }).click()
  }

  async openPayments(reference: string): Promise<void> {
    await this.rowByReference(reference).getByRole('link', { name: `View payments for ${reference}` }).click()
  }

  async editDisplayName(reference: string): Promise<void> {
    await this.rowByReference(reference).getByRole('button', { name: `Edit name ${reference}` }).click()
  }

  async fillDisplayName(value: string): Promise<void> {
    await this.page.getByLabel(/Display name for /).fill(value)
  }

  async saveDisplayName(reference: string): Promise<void> {
    await this.page.getByRole('button', { name: `Save name ${reference}` }).click()
  }

  async openImport(): Promise<void> {
    await this.byTestId('action-import-merchants').click()
    await expect(this.byTestId('merchant-import-panel')).toBeVisible()
  }

  csvInput() {
    return this.page.getByLabel('Upload CSV')
  }

  orgTree(): Locator {
    return this.page.getByTestId('org-tree')
  }

  treeItem(name: string | RegExp): Locator {
    return this.page.getByRole('treeitem', { name })
  }

  async expandTreeItem(name: string | RegExp): Promise<void> {
    const item = this.treeItem(name)
    await item.click()
    await expect(item).toHaveAttribute('aria-expanded', 'true')
  }

  async collapseTreeItem(name: string | RegExp): Promise<void> {
    const item = this.treeItem(name)
    await item.click()
    await expect(item).toHaveAttribute('aria-expanded', 'false')
  }

  async expandTreeItemWithKeyboard(name: string | RegExp): Promise<void> {
    const item = this.treeItem(name)
    await item.focus()
    await item.press('ArrowRight')
    await expect(item).toHaveAttribute('aria-expanded', 'true')
  }
}
