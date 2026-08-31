import type { Locator, Page } from '@playwright/test'

export class Sidebar {
  constructor(private readonly page: Page) {}

  async openMerchants(): Promise<void> {
    await this.page.getByTestId('nav-link-merchants').click()
  }

  merchants(): Locator {
    return this.page.getByTestId('nav-link-merchants')
  }

  checkoutLab(): Locator {
    return this.page.getByTestId('nav-link-checkout-lab')
  }

  eventLab(): Locator {
    return this.page.getByTestId('nav-link-event-lab')
  }

  overview(): Locator { return this.page.getByTestId('nav-link-overview') }
  users(): Locator { return this.page.getByTestId('nav-link-users') }
  audit(): Locator { return this.page.getByTestId('nav-link-audit') }
  support(): Locator { return this.page.getByTestId('nav-link-support') }
  mirrorLab(): Locator { return this.page.getByTestId('nav-link-mirror-lab') }
  rlsLab(): Locator { return this.page.getByTestId('nav-link-rls-lab') }

  paymentOrders(): Locator {
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
    await this.checkoutLab().click()
  }

  async openMirrorLab(): Promise<void> {
    await this.page.getByTestId('nav-link-mirror-lab').click()
  }

  async openRlsLab(): Promise<void> {
    await this.page.getByTestId('nav-link-rls-lab').click()
  }

}
