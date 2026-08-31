import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { ConfirmModal } from './components/ConfirmModal'
import { PinChallengeComponent } from './components/PinChallengeComponent'
import { EvidenceCarouselComponent } from './components/EvidenceCarouselComponent'

export class PaymentDetailPage extends BasePage {
  readonly confirm: ConfirmModal
  readonly pinChallenge: PinChallengeComponent
  readonly evidenceCarousel: EvidenceCarouselComponent

  constructor(page: Page) {
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

  amount(): Locator {
    return this.byTestId('payment-amount')
  }

  heading(name: string): Locator { return this.byTestId('payment-order-detail').getByRole('heading', { name, exact: true }) }
  fieldLabel(name: string): Locator { return this.page.getByText(name, { exact: true }) }
  displayedReference(reference: string): Locator { return this.byTestId('payment-order-detail').getByText(reference, { exact: true }) }
  expirationCountdown(): Locator { return this.byTestId('expiration-countdown') }
  expirationCountdownRemaining(): Locator { return this.byTestId('expiration-countdown-remaining') }
  lifecycleAction(action: 'authorize' | 'capture' | 'cancel'): Locator { return this.byTestId(`lifecycle-${action}`) }

  createdAt(): Locator {
    return this.byTestId('payment-created-at')
  }

  statusInDetail(label: string): Locator {
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

  lifecycleAmountInput(): Locator {
    return this.byTestId('lifecycle-amount-input')
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
      await this.lifecycleAmountInput().fill(String(amountMinor))
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

  async uploadEvidencePayload(file: Parameters<Locator['setInputFiles']>[0]): Promise<void> {
    await this.byTestId('evidence-upload-input').setInputFiles(file)
    await this.byTestId('evidence-upload-submit').click()
  }

  evidenceFile(name: string): Locator { return this.byTestId('evidence-file-name').filter({ hasText: name }) }
  evidenceFileName(): Locator { return this.byTestId('evidence-file-name') }
  evidenceDownload(): Locator { return this.byTestId('evidence-download') }

  async addNote(body: string): Promise<void> {
    const editor = this.noteEditor()
    await editor.click()
    await editor.fill(body)
    await this.byTestId('payment-note-submit').click()
  }

  noteByText(text: string): Locator {
    return this.byTestId('payment-note-item').filter({ hasText: text })
  }

  notesForm(): Locator {
    return this.byTestId('payment-note-body')
  }

  internalNotes(): Locator { return this.byTestId('payment-internal-notes') }
  noteEditor(): Locator {
    // Nuxt UI renders this rich-text editing surface without an accessible role.
    return this.notesForm().locator('[contenteditable="true"]')
  }
  errorAlert(): Locator { return this.page.getByRole('alert').or(this.byTestId('error-state')) }
  noteScripts(): Locator {
    // Security oracle: this inspects rendered DOM nodes, which have no accessible locator.
    return this.internalNotes().locator('script')
  }

  currentStatus(): Locator {
    // During migration either polling state or current status is rendered, never both as the oracle target.
    return this.page.locator('[data-testid="payment-status-polling"] [data-status], [data-testid="payment-status-current"]').first()
  }

  async refreshStatus(): Promise<void> {
    await this.byTestId('payment-status-refresh').click()
  }

  async enableAutoRefresh(): Promise<void> {
    await this.page.getByLabel('Auto refresh').click()
  }

  async openHistoryTab(): Promise<void> {
    await this.historyTab().click()
  }

  historyTab(): Locator {
    return this.page.getByRole('tab', { name: 'History' })
  }

  historyEntry(text: string): Locator {
    return this.page.getByText(text, { exact: true })
  }

  emptyHistory(): Locator {
    return this.page.getByText('No lifecycle history recorded.')
  }

  dualControlHint(): Locator {
    return this.byTestId('lifecycle-refund-dual-control-hint')
  }

  refundApprovalCreate(): Locator {
    return this.byTestId('refund-approval-create')
  }

  refundApprovalApprove(): Locator {
    return this.byTestId('refund-approval-approve')
  }

  refundApprovalPending(): Locator {
    return this.page.getByText('PENDING')
  }

  refundApprovalSelfApprovalError(): Locator {
    return this.page.getByText(/dual_control_self_approve|Maker cannot approve/i)
  }

  deliveryCard(): Locator { return this.byTestId('eventlab-delivery-card') }
  deliveryStatus(): Locator { return this.byTestId('eventlab-delivery-status') }
  deliveryPending(): Locator { return this.byTestId('eventlab-delivery-pending') }
  deliveryProcessed(): Locator { return this.byTestId('eventlab-delivery-processed') }
  deliveryEmpty(): Locator { return this.byTestId('eventlab-delivery-empty') }
  deliveryState(): Locator { return this.deliveryPending().or(this.deliveryProcessed()).or(this.deliveryEmpty()) }

  /** Approve a pending refund approval (the spec owns the outcome assertion). */
  async approveRefundApproval(): Promise<void> {
    await this.assertNoDevErrorOverlay()
    await this.byTestId('refund-approval-approve').click()
  }

  historyTimeline(): Locator {
    return this.page.getByTestId('payment-history-timeline')
  }

  async copyClientReference(): Promise<void> {
    await this.byTestId('copy-payment-reference').click()
  }
}
