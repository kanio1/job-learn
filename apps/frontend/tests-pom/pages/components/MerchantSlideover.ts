import { expect, type Page } from '@playwright/test'
import { ConfirmModal } from './ConfirmModal'

/**
 * Merchant 360 USlideover on the registry list.
 *
 * What: open/close/sections. Not 201/status-code claims (those stay in the spec).
 * Layer: e2e. Seed: unique merchant from the spec.
 */
export class MerchantSlideover {
  readonly confirm: ConfirmModal

  constructor(private readonly page: Page) {
    this.confirm = new ConfirmModal(page)
  }

  dialog() {
    return this.page.getByRole('dialog', { name: 'Merchant 360' })
  }

  async expectOpen(): Promise<void> {
    await expect(this.dialog()).toBeVisible()
    await expect(this.dialog().getByRole('heading', { name: 'Information' })).toBeVisible()
    await expect(this.dialog().getByRole('heading', { name: 'Risk' })).toBeVisible()
    await expect(this.dialog().getByRole('heading', { name: 'Payments' })).toBeVisible()
    await expect(this.dialog().getByRole('heading', { name: 'Notes' })).toBeVisible()
    await expect(this.dialog().getByRole('heading', { name: 'History' })).toBeVisible()
  }

  timeline() {
    return this.dialog().getByTestId('merchant-360-timeline')
  }

  async expectClosed(): Promise<void> {
    await expect(this.dialog()).toHaveCount(0)
  }

  async closeWithEscape(): Promise<void> {
    await this.page.keyboard.press('Escape')
  }

  async openSuspendConfirm(): Promise<void> {
    await this.dialog().getByRole('button', { name: /^Suspend / }).click()
    await this.confirm.expectOpen(/Suspend/)
  }

  async openPaymentOrders(): Promise<void> {
    await this.dialog().getByRole('link', { name: 'View payment orders' }).click()
  }
}
