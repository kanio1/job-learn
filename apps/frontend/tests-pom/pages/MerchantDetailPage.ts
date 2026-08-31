import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { ConflictDiffComponent } from './components/ConflictDiffComponent'
import { UnsavedGuardDialog } from './components/UnsavedGuardDialog'

export class MerchantDetailPage extends BasePage {
  readonly conflict: ConflictDiffComponent
  readonly unsaved: UnsavedGuardDialog

  constructor(page: Page) {
    super(page)
    this.conflict = new ConflictDiffComponent(page)
    this.unsaved = new UnsavedGuardDialog(page)
  }

  async gotoMerchant(merchantId: string): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('merchant-detail-panel')).toBeVisible()
    await expect(this.byTestId('merchant-name')).toBeVisible()
  }

  statusBadge(): Locator {
    return this.byTestId('merchant-status-badge')
  }

  reference(): Locator { return this.byTestId('merchant-reference') }
  riskToggle(): Locator { return this.byTestId('merchant-risk-toggle') }
  reloadButton(): Locator { return this.page.getByRole('button', { name: 'Reload' }) }

  async activate(): Promise<void> {
    await this.byTestId('action-activate-merchant').click()
  }

  async suspend(): Promise<void> {
    await this.byTestId('action-suspend-merchant').click()
  }

  async openPayments(): Promise<void> {
    await this.byTestId('merchant-payment-orders-link').click()
  }

  async reloadAfterConflict(): Promise<void> {
    await this.reloadButton().click()
  }

  async toggleRisk(): Promise<void> {
    await this.byTestId('merchant-risk-toggle').click()
  }

  riskStatus(): Locator {
    return this.byTestId('merchant-risk-status')
  }

  async fillContact(fields: { displayName?: string, contactPhone?: string, contactAddress?: string }): Promise<void> {
    if (fields.displayName !== undefined) {
      await this.byTestId('merchant-display-name-input').fill(fields.displayName)
    }
    if (fields.contactPhone !== undefined) {
      await this.byTestId('merchant-contact-phone-input').fill(fields.contactPhone)
    }
    if (fields.contactAddress !== undefined) {
      await this.byTestId('merchant-contact-address-input').fill(fields.contactAddress)
    }
  }

  async saveContact(): Promise<void> {
    await this.byTestId('merchant-save').click()
  }

  async goBackToList(): Promise<void> {
    await this.byTestId('merchant-back-to-list').click()
  }

  phoneInput(): Locator {
    return this.byTestId('merchant-contact-phone-input')
  }
}
