import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { ConfirmModal } from './components/ConfirmModal'

export class PaymentDetailPage extends BasePage {
  readonly confirm: ConfirmModal

  constructor(page: import('@playwright/test').Page) {
    super(page)
    this.confirm = new ConfirmModal(page)
  }

  async gotoOrder(merchantId: string, paymentOrderId: string): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('payment-order-detail')).toBeVisible()
  }

  statusInDetail(label: string) {
    return this.byTestId('payment-order-detail').getByText(label, { exact: true })
  }

  async openLifecycle(action: 'authorize' | 'capture' | 'cancel'): Promise<void> {
    await this.assertNoDevErrorOverlay()
    await this.byTestId(`lifecycle-${action}`).click()
    await expect(this.page.getByTestId('lifecycle-submit-button')).toBeVisible()
  }

  async fillIfMatch(value: string): Promise<void> {
    await this.page.getByLabel('If-Match').fill(value)
  }

  async ifMatchValue(): Promise<string> {
    return this.page.getByLabel('If-Match').inputValue()
  }

  async submitLifecycle(): Promise<void> {
    await this.byTestId('lifecycle-submit-button').click()
  }

  async authorize(): Promise<void> {
    await this.openLifecycle('authorize')
    await this.submitLifecycle()
  }

  async capture(amountMinor?: number): Promise<void> {
    await this.openLifecycle('capture')
    if (amountMinor != null) {
      await this.byTestId('lifecycle-amount-input').fill(String(amountMinor))
    }
    await this.submitLifecycle()
  }

  async cancel(): Promise<void> {
    await this.openLifecycle('cancel')
    await this.submitLifecycle()
    await this.confirm.expectOpen(/Confirm Cancel/)
    await this.confirm.confirm()
  }

  async openCancelThenDismiss(): Promise<void> {
    await this.openLifecycle('cancel')
    await this.submitLifecycle()
    await this.confirm.expectOpen(/Confirm Cancel/)
    await this.confirm.dismiss()
  }

  async uploadEvidence(filePath: string): Promise<void> {
    await this.byTestId('evidence-upload-input').setInputFiles(filePath)
    await this.byTestId('evidence-upload-submit').click()
  }

  async addNote(body: string): Promise<void> {
    await this.byTestId('payment-note-body').fill(body)
    await this.byTestId('payment-note-submit').click()
  }

  async expectNoteVisible(text: string): Promise<void> {
    await expect(this.byTestId('payment-note-item').filter({ hasText: text })).toBeVisible()
  }

  currentStatus() {
    return this.page.locator('[data-testid="payment-status-polling"] [data-status], [data-testid="payment-status-current"]').first()
  }

  async refreshStatus(): Promise<void> {
    await this.byTestId('payment-status-refresh').click()
  }

  async enableAutoRefresh(): Promise<void> {
    await this.page.getByLabel('Auto refresh').click()
  }
}
