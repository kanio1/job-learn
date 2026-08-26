import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class AuditPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/audit')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByRole('heading', { name: 'Audit log' }).first()).toBeVisible()
    await expect(this.byTestId('audit-filters')).toBeVisible()
  }

  async export(): Promise<void> {
    await this.byTestId('export-audit-log').click()
  }

  async openFirstRow(): Promise<void> {
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
