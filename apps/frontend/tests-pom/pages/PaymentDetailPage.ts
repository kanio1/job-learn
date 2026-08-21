import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { ConfirmModal } from './components/ConfirmModal'
import { PinChallengeComponent } from './components/PinChallengeComponent'
import { EvidenceCarouselComponent } from './components/EvidenceCarouselComponent'

export class PaymentDetailPage extends BasePage {
  readonly confirm: ConfirmModal
  readonly pinChallenge: PinChallengeComponent
  readonly evidenceCarousel: EvidenceCarouselComponent

  constructor(page: import('@playwright/test').Page) {
    super(page)
    this.confirm = new ConfirmModal(page)
    this.pinChallenge = new PinChallengeComponent(page)
    this.evidenceCarousel = new EvidenceCarouselComponent(page)
  }

  async gotoOrder(merchantId: string, paymentOrderId: string, query = ''): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}${query}`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('payment-order-detail')).toBeVisible()
  }

  amount(): import('@playwright/test').Locator {
    return this.byTestId('payment-amount')
  }

  createdAt(): import('@playwright/test').Locator {
    return this.byTestId('payment-created-at')
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
    const editor = this.byTestId('payment-note-body').locator('[contenteditable="true"]')
    await editor.click()
    await editor.fill(body)
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

  async openHistoryTab(): Promise<void> {
    await this.page.getByRole('tab', { name: 'History' }).click()
  }

  historyTimeline() {
    return this.page.getByTestId('payment-history-timeline')
  }

  async copyClientReference(): Promise<void> {
    await this.byTestId('copy-payment-reference').click()
  }
}
