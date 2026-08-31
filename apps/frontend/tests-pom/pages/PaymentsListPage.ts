import { expect, type Locator } from '@playwright/test'
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

  /** Click the toolbar's expiration-sweep button (the POST oracle stays in the spec). */
  async runExpirationSweep(): Promise<void> {
    await this.byTestId('run-expiration-sweep').click()
  }

  expirationSweepComplete(): Locator { return this.page.getByText('Expiration sweep complete', { exact: true }) }
  expirationSweepCount(count: number | undefined): Locator { return this.page.getByText(`${count} order(s) expired`, { exact: true }) }
  heading(): Locator { return this.page.getByRole('heading', { name: 'Payment Orders', exact: true }) }
  columnHeader(name: string): Locator { return this.page.getByRole('columnheader', { name, exact: true }) }
  offlineBanner(): Locator { return this.byTestId('payments-offline-banner') }
  asyncExportStatus(): Locator { return this.byTestId('async-export-status') }
  totalOrdersCard(): Locator { return this.page.getByRole('region', { name: 'Total orders' }) }
  totalOrdersValue(total: number): Locator { return this.totalOrdersCard().getByText(String(total), { exact: true }) }

  async openCreate(): Promise<void> {
    await this.page.getByRole('link', { name: 'New payment' }).click()
  }

  async sortByAmount(): Promise<void> {
    await this.page.getByRole('columnheader', { name: /Amount/i }).click()
  }

  amountColumnButton(): Locator { return this.page.getByRole('button', { name: 'Amount', exact: true }) }
  paymentAccessDenied(): Locator {
    return this.page.getByRole('alert').filter({ hasText: 'You do not have permission to view payment orders' })
  }

  async gotoPage(displayPage: number): Promise<void> {
    await this.byTestId('payment-orders-pagination')
      .getByRole('button', { name: new RegExp(`Page ${displayPage}|^${displayPage}$`) })
      .click()
  }

  referenceInTable(reference: string): Locator {
    return this.byTestId('payment-orders-table').getByText(reference)
  }

  statusBadgeForReference(reference: string) {
    // Table is a Nuxt UI grid in one route and a semantic table in another.
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

  async dragCardTo(paymentOrderId: string, status: string): Promise<void> {
    const dataTransfer = await this.page.evaluateHandle(() => new DataTransfer())
    const card = this.card(paymentOrderId)
    const target = this.stage(status)
    await card.dispatchEvent('dragstart', { dataTransfer })
    await target.dispatchEvent('dragover', { dataTransfer })
    await target.dispatchEvent('drop', { dataTransfer })
  }

  statusChart() {
    return this.byTestId('payment-status-chart')
  }

  statusLegend(status: string) {
    return this.statusChart().getByRole('rowheader', { name: new RegExp(`^${status} `) })
  }
}
