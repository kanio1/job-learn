import { expect, type Page } from '@playwright/test'

export class Sidebar {
  constructor(private readonly page: Page) {}

  async openMerchants(): Promise<void> {
    await this.page.getByTestId('nav-link-merchants').click()
  }

  paymentOrders() {
    return this.page.getByTestId('nav-link-payment-orders')
  }

  async openPaymentOrders(): Promise<void> {
    await this.paymentOrders().click()
  }

  async openUsers(): Promise<void> {
    await this.page.getByTestId('nav-link-users').click()
  }

  async openAudit(): Promise<void> {
    await this.page.getByTestId('nav-link-audit').click()
  }

  async openErrorLab(): Promise<void> {
    await this.page.getByTestId('nav-link-error-lab').click()
  }

  async openCheckoutLab(): Promise<void> {
    await this.page.getByTestId('nav-link-checkout-lab').click()
  }

  async openMirrorLab(): Promise<void> {
    await this.page.getByTestId('nav-link-mirror-lab').click()
  }

  async openRlsLab(): Promise<void> {
    await this.page.getByTestId('nav-link-rls-lab').click()
  }

  async expectUsersVisible(visible: boolean): Promise<void> {
    if (visible) {
      await expect(this.page.getByTestId('nav-link-users')).toBeVisible()
    } else {
      await expect(this.page.getByTestId('nav-link-users')).toHaveCount(0)
    }
  }

  async expectAuditVisible(visible: boolean): Promise<void> {
    if (visible) {
      await expect(this.page.getByTestId('nav-link-audit')).toBeVisible()
    } else {
      await expect(this.page.getByTestId('nav-link-audit')).toHaveCount(0)
    }
  }
}
