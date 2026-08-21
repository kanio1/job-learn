import { type Locator, type Page } from '@playwright/test'

export class PaymentFiltersComponent {
  constructor(private readonly page: Page) {}

  private filterInput(testId: string): Locator {
    return this.page.locator(`[data-testid="${testId}"] input, input[data-testid="${testId}"]`).first()
  }

  applyButton(): Locator {
    return this.page.getByTestId('payment-filter-apply')
  }

  clearButton(): Locator {
    return this.page.getByTestId('payment-filter-clear')
  }

  async apply(): Promise<void> {
    await this.applyButton().click()
  }

  async clear(): Promise<void> {
    await this.clearButton().click()
  }

  async applyStatus(label: string): Promise<void> {
    await this.page.getByTestId('payment-filter-status').click()
    await this.page.getByRole('option', { name: label }).click()
    await this.apply()
  }

  async applyCurrency(label: string): Promise<void> {
    await this.page.getByTestId('payment-filter-currency').click()
    await this.page.getByRole('option', { name: label }).click()
    await this.apply()
  }

  async applyMinAmount(minAmount: number): Promise<void> {
    await this.filterInput('payment-filter-min-amount').fill(String(minAmount))
    await this.apply()
  }

  async applyLargeEurCaptured(): Promise<void> {
    await this.applyStatus('Captured')
    await this.applyCurrency('EUR')
    await this.applyMinAmount(10000)
  }

  columnCheckbox(label: string): Locator {
    return this.page.getByRole('checkbox', { name: label })
  }

  async uncheckColumn(label: string): Promise<void> {
    const box = this.columnCheckbox(label)
    if (await box.isChecked()) {
      await box.uncheck()
    }
  }
}
