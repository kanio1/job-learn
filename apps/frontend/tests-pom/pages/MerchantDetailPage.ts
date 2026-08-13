import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class MerchantDetailPage extends BasePage {
  async gotoMerchant(merchantId: string): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('merchant-detail-panel')).toBeVisible()
    await expect(this.byTestId('merchant-name')).toBeVisible()
  }

  async expectStatus(label: 'Draft' | 'Active' | 'Suspended'): Promise<void> {
    await expect(this.byTestId('merchant-status-badge')).toContainText(label)
  }

  async activate(): Promise<void> {
    await this.byTestId('action-activate-merchant').click()
  }

  async suspend(): Promise<void> {
    await this.byTestId('action-suspend-merchant').click()
  }

  async openPayments(): Promise<void> {
    await this.byTestId('merchant-payment-orders-link').click()
  }

  async toggleRisk(): Promise<void> {
    await this.byTestId('merchant-risk-toggle').click()
  }

  async expectRiskFlagged(flagged: boolean): Promise<void> {
    const label = flagged ? 'Risk flagged' : 'No risk flag'
    await expect(this.byTestId('merchant-risk-status')).toContainText(label)
  }
}
