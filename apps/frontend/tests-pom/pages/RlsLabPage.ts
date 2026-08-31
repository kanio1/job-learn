import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class RlsLabPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/rls-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByText('Java WHERE is not RLS')).toBeVisible()
    await expect(this.byTestId('rls-lab-items-table')).toBeVisible()
  }

  async probe(itemId?: string): Promise<void> {
    await this.assertNoDevErrorOverlay()
    if (itemId) {
      // Nuxt UI wraps this test-id differently across Input and InputGroup variants.
      const input = this.page.locator('[data-testid="rls-lab-probe-id"] input, input[data-testid="rls-lab-probe-id"]').first()
      await input.fill(itemId)
    }
    await this.byTestId('rls-lab-probe').click()
  }

  async loadCompare(): Promise<void> {
    await this.assertNoDevErrorOverlay()
    await this.byTestId('rls-lab-compare-load').click()
  }

  comparePanel(): Locator { return this.byTestId('rls-lab-compare-panel') }
  restrictedWithoutTenant(): Locator { return this.byTestId('rls-lab-compare-restricted-no-tenant') }
  unprotectedCount(): Locator { return this.byTestId('rls-lab-compare-unprotected') }
  item(text: string): Locator { return this.page.getByText(text, { exact: true }) }
  itemsTable(): Locator { return this.byTestId('rls-lab-items-table') }
}
