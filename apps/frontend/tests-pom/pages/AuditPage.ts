import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class AuditPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/audit')
  }

  async expectLoaded(): Promise<void> {
    // The dashboard navbar and content both expose this name; the content h1 is the focus target.
    await expect(this.page.locator('h1[tabindex="-1"]', { hasText: 'Audit log' })).toBeVisible()
    await expect(this.byTestId('audit-filters')).toBeVisible()
  }

  async export(): Promise<void> {
    await this.byTestId('export-audit-log').click()
  }

  table(): Locator { return this.byTestId('audit-table') }
  entryDrawer(): Locator { return this.byTestId('audit-entry-drawer') }

  async openFirstRow(): Promise<void> {
    // Rows have dynamic test ids; the first visible result is the documented audit-list action.
    await this.page.locator('[data-testid^="audit-row-"]').first().click()
  }

  async applyActionFilter(label: string): Promise<void> {
    await this.byTestId('audit-filter-action').click()
    await this.page.getByRole('option', { name: label }).click()
    await this.page.getByRole('button', { name: 'Apply filters' }).click()
  }

  async gotoEntry(eventId: string): Promise<void> {
    await super.goto(`/admin/audit?entry=${encodeURIComponent(eventId)}`)
  }
}
