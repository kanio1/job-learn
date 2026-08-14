import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class RlsLabPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/rls-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByText('Java WHERE is not RLS')).toBeVisible()
    await expect(this.byTestId('rls-lab-items-table')).toBeVisible()
  }

  async probe(itemId?: string): Promise<void> {
    await this.assertNoDevErrorOverlay()
    if (itemId) {
      const input = this.page.locator('[data-testid="rls-lab-probe-id"] input, input[data-testid="rls-lab-probe-id"]').first()
      await input.fill(itemId)
    }
    await this.byTestId('rls-lab-probe').click()
  }

  async loadCompare(): Promise<void> {
    await this.assertNoDevErrorOverlay()
    await this.byTestId('rls-lab-compare-load').click()
  }
}
