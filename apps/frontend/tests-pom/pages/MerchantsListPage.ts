import { expect, type Download, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'
import { waitForBffResponse } from '../utils/wait-bff'

export class MerchantsListPage extends BasePage {
  override async goto(query = ''): Promise<void> {
    await super.goto(`/admin/merchants${query}`)
  }

  async expectLoaded(): Promise<void> {
    await this.expectHeading('Merchants')
    await expect(this.page.getByTestId('loading-state')).toHaveCount(0)
  }

  /** Registry caption — the rendered `totalElements merchant(s)` line. */
  caption(): Locator {
    return this.byTestId('merchant-registry-caption')
  }

  registryTable(): Locator {
    return this.page.getByRole('table').filter({
      has: this.page.getByRole('columnheader', { name: 'Reference' }),
    })
  }
  heading(name: string): Locator { return this.page.getByRole('heading', { name, exact: true }) }
  columnHeader(name: string): Locator { return this.page.getByRole('columnheader', { name, exact: true }) }
  statusFilter(): Locator { return this.page.getByLabel('Filter status') }
  filterOption(label: string): Locator { return this.page.getByRole('option', { name: label }) }
  applyButton(): Locator { return this.byTestId('merchant-filter-apply') }
  filteredEmpty(): Locator { return this.page.getByText('No merchants match the current filters.') }
  loadingStatus(): Locator { return this.page.getByRole('status', { name: 'Loading merchants…' }) }
  createButton(): Locator { return this.byTestId('action-create-merchant') }
  bulkActivateButton(): Locator { return this.byTestId('merchant-bulk-activate') }
  importButton(): Locator { return this.page.getByRole('button', { name: /Import/ }) }
  riskToggle(): Locator { return this.byTestId('merchant-risk-toggle') }
  tenantColumn(): Locator { return this.page.getByRole('columnheader', { name: 'Tenant' }) }
  mutationButtons(): Locator { return this.page.getByRole('button', { name: /Activate|Suspend|Create merchant/ }) }
  activateButtons(): Locator { return this.page.getByRole('button', { name: /^Activate / }) }
  selectionCheckboxes(): Locator { return this.page.getByRole('checkbox', { name: /Select / }) }
  activationFailure(): Locator { return this.page.getByText('Activation failed', { exact: true }) }
  nameInput(): Locator { return this.byTestId('merchant-name-input') }
  saveButton(): Locator { return this.byTestId('merchant-save') }
  saveLikeButtons(): Locator { return this.page.getByRole('button', { name: /save|zapisz|spara/i }) }
  errorState(): Locator { return this.byTestId('error-state') }
  createForm(): Locator { return this.byTestId('create-merchant-form') }
  tenantReferenceInput(): Locator { return this.byTestId('create-merchant-tenant-reference') }
  referenceInput(): Locator { return this.page.getByLabel('Merchant reference') }
  createAlert(): Locator { return this.page.getByRole('alert').filter({ hasText: /already exists/i }) }
  openButton(reference: string): Locator { return this.page.getByRole('button', { name: `Open ${reference}` }) }
  tokenRows(token: string): Locator { return this.registryTable().getByRole('row').filter({ hasText: token }) }

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

  tableText(text: string): Locator { return this.registryTable().getByText(text, { exact: true }) }

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

  createFieldError(message: string | RegExp): Locator {
    return this.byTestId('create-merchant-form').getByText(message)
  }

  riskBadgeFor(displayName: string): Locator {
    const row = this.page.getByRole('row').filter({ hasText: displayName })
    return row.getByTestId('merchant-risk-badge')
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
    await this.statusFilter().click()
    await this.filterOption(label).click()
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

  async sortBy(column: RegExp): Promise<void> {
    await this.page.getByRole('columnheader', { name: column }).click()
  }

  rowCell(text: string): Locator { return this.registryTable().getByRole('cell', { name: text, exact: true }) }

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

  csvInput(): Locator {
    return this.page.getByLabel('Upload CSV')
  }

  importValidCount(): Locator { return this.byTestId('merchant-import-valid') }
  importRejectedCount(): Locator { return this.byTestId('merchant-import-rejected') }
  importCommitButton(): Locator { return this.byTestId('merchant-import-commit') }
  importedMerchantName(name: string): Locator { return this.page.getByText(name, { exact: true }) }

  async downloadRejected(): Promise<Download> {
    const download = this.page.waitForEvent('download')
    await this.page.getByRole('button', { name: 'Download rejected' }).click()
    return download
  }

  async commitImport(): Promise<void> {
    await this.importCommitButton().click()
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
