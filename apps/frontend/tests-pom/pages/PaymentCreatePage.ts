import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class PaymentCreatePage extends BasePage {
  async gotoForMerchant(merchantId: string): Promise<void> {
    await super.goto(`/admin/merchants/${merchantId}/payments/new`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByRole('heading', { name: 'New Payment Order' })).toBeVisible()
    await expect(this.byTestId('create-payment-order-form')).toBeVisible()
  }

  async fillIdempotencyKey(key: string): Promise<void> {
    await this.page.getByLabel('Idempotency Key').fill(key)
  }

  async fillAmount(amountMinor: number): Promise<void> {
    await this.page.getByLabel('Amount (minor units)').fill(String(amountMinor))
  }

  async chooseCurrency(code: string): Promise<void> {
    await this.page.getByLabel('Currency').click()
    await this.page.getByRole('option', { name: code }).click()
  }

  async fillReference(reference: string): Promise<void> {
    await this.page.getByLabel('Client Order Reference').fill(reference)
  }

  async submit(): Promise<void> {
    await this.byTestId('action-create-payment-order').click()
  }
}
