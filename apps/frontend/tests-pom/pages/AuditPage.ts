import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class AuditPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/audit')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByRole('heading', { name: 'Audit log' })).toBeVisible()
    await expect(this.byTestId('audit-filters')).toBeVisible()
  }

  async export(): Promise<void> {
    await this.byTestId('export-audit-log').click()
  }

  async openFirstRow(): Promise<void> {
    await this.page.locator('[data-testid^="audit-row-"]').first().click()
  }
}
