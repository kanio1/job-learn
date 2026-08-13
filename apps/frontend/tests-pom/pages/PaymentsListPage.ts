import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class PaymentsListPage extends BasePage {
  async gotoForMerchant(merchantId: string): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}/payments`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('payment-orders-table')).toBeVisible()
  }

  async openCreate(): Promise<void> {
    await this.page.getByRole('link', { name: 'New payment' }).click()
  }

  async applyDateFilter(fromDate: string, toDate: string): Promise<void> {
    await this.page.getByLabel('Created from').fill(fromDate)
    await this.page.getByLabel('Created to').fill(toDate)
    await this.byTestId('payment-filter-apply').click()
  }

  async applyStatusFilter(label: string): Promise<void> {
    await this.page.getByLabel('Status').click()
    await this.page.getByRole('option', { name: label }).click()
    await this.byTestId('payment-filter-apply').click()
  }

  async filterByClientReference(reference: string): Promise<void> {
    await this.page.getByLabel('Client order reference').fill(reference)
    await this.byTestId('payment-filter-apply').click()
  }

  async exportCsv(): Promise<void> {
    await this.byTestId('export-payment-orders-csv').click()
  }

  async expectReferenceVisible(reference: string): Promise<void> {
    await expect(this.byTestId('payment-orders-table').getByText(reference)).toBeVisible()
  }
}
