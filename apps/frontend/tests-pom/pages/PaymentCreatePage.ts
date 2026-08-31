import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class PaymentCreatePage extends BasePage {
  async gotoForMerchant(merchantId: string): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}/payments/new`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByRole('heading', { name: 'New Payment Order' })).toBeVisible()
    await expect(this.byTestId('create-payment-order-form')).toBeVisible()
  }

  form(): Locator { return this.byTestId('create-payment-order-form') }

  async fillIdempotencyKey(key: string): Promise<void> {
    await this.page.getByLabel('Idempotency Key').fill(key)
  }

  async fillAmount(amountMinor: number): Promise<void> {
    await this.page.getByLabel('Amount (minor units)').fill(String(amountMinor))
  }

  async next(): Promise<void> {
    await this.page.getByRole('button', { name: 'Next' }).click()
  }

  async chooseCurrency(code: string): Promise<void> {
    await this.page.getByRole('combobox', { name: 'Currency' }).click()
    await this.page.getByRole('option', { name: code }).click()
  }

  async fillReference(reference: string): Promise<void> {
    await this.page.getByLabel('Client Order Reference').fill(reference)
  }

  async submit(): Promise<void> {
    await this.byTestId('action-create-payment-order').click()
  }

  amountError(): Locator { return this.page.getByText('Amount must be at least 1', { exact: true }) }
  review(): Locator { return this.byTestId('create-payment-order-review') }
  submitButton(): Locator { return this.byTestId('action-create-payment-order') }
}
