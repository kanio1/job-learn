import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { PaymentFiltersComponent } from './components/PaymentFiltersComponent'
import { SavedViewsComponent } from './components/SavedViewsComponent'

export class PaymentsListPage extends BasePage {
  readonly filters = new PaymentFiltersComponent(this.page)
  readonly views = new SavedViewsComponent(this.page)

  async gotoForMerchant(merchantId: string, query = ''): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}/payments${query}`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('payment-orders-table')).toBeVisible()
  }

  async openCreate(): Promise<void> {
    await this.page.getByRole('link', { name: 'New payment' }).click()
  }

  private filterInput(testId: string) {
    return this.page.locator(`[data-testid="${testId}"] input, input[data-testid="${testId}"]`).first()
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

  async applyAmountFilter(minAmount: number, maxAmount: number): Promise<void> {
    await this.filterInput('payment-filter-min-amount').fill(String(minAmount))
    await this.filterInput('payment-filter-max-amount').fill(String(maxAmount))
    await this.byTestId('payment-filter-apply').click()
  }

  async applyCurrencyFilter(label: string): Promise<void> {
    await this.page.getByLabel('Currency').click()
    await this.page.getByRole('option', { name: label }).click()
    await this.byTestId('payment-filter-apply').click()
  }

  async filterByClientReference(reference: string): Promise<void> {
    await this.filterInput('payment-filter-reference').fill(reference)
    await this.byTestId('payment-filter-apply').click()
  }

  async clearFilters(): Promise<void> {
    await this.byTestId('payment-filter-clear').click()
  }

  async sortByAmount(): Promise<void> {
    await this.page.getByRole('columnheader', { name: /Amount/i }).click()
  }

  async gotoPage(displayPage: number): Promise<void> {
    await this.byTestId('payment-orders-pagination')
      .getByRole('button', { name: new RegExp(`Page ${displayPage}|^${displayPage}$`) })
      .click()
  }

  async expectReferenceVisible(reference: string): Promise<void> {
    await expect(this.byTestId('payment-orders-table').getByText(reference)).toBeVisible()
  }

  async expectReferenceHidden(reference: string): Promise<void> {
    await expect(this.byTestId('payment-orders-table').getByText(reference)).toHaveCount(0)
  }

  statusBadgeForReference(reference: string) {
    return this.byTestId('payment-orders-table')
      .locator('[role="row"], tr')
      .filter({ hasText: reference })
      .getByTestId('payment-status-badge')
  }

  async exportCsv(): Promise<void> {
    await this.byTestId('export-payment-orders-csv').click()
  }

  async exportAsync(): Promise<void> {
    await this.byTestId('export-payment-orders-async').click()
  }

  async openBoard(): Promise<void> {
    await this.byTestId('payments-view-board').click()
    await expect(this.byTestId('payment-kanban')).toBeVisible()
  }

  async openCalendar(): Promise<void> {
    await this.byTestId('payments-view-calendar').click()
    await expect(this.byTestId('payment-expiry-calendar')).toBeVisible()
  }

  calendar() {
    return this.byTestId('payment-expiry-calendar')
  }

  card(paymentOrderId: string) {
    return this.byTestId(`payment-card-${paymentOrderId}`)
  }

  stage(status: string) {
    return this.byTestId(`stage-${status}`)
  }

  async moveCardTo(paymentOrderId: string, status: string): Promise<void> {
    const card = this.card(paymentOrderId)
    await card.getByRole('button', { name: /^Move / }).click()
    await this.page.getByRole('menuitem', { name: `Move to ${status}` }).click()
  }

  statusChart() {
    return this.byTestId('payment-status-chart')
  }

  statusLegend(status: string) {
    return this.statusChart().getByRole('rowheader', { name: new RegExp(`^${status} `) })
  }
}
