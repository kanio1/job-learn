import { type Locator, type Page } from '@playwright/test'

export class PaymentFiltersComponent {
  constructor(private readonly page: Page) {}

  private filterInput(testId: string): Locator {
    // Nuxt UI may put the test id on either the input or its wrapper; each filter has one input.
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

  async applyDateRange(fromDate: string, toDate: string): Promise<void> {
    await this.page.getByLabel('Created from').fill(fromDate)
    await this.page.getByLabel('Created to').fill(toDate)
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

  async applyAmountRange(minAmount: number, maxAmount: number): Promise<void> {
    await this.filterInput('payment-filter-min-amount').fill(String(minAmount))
    await this.filterInput('payment-filter-max-amount').fill(String(maxAmount))
    await this.apply()
  }

  async applyClientReference(reference: string): Promise<void> {
    await this.filterInput('payment-filter-reference').fill(reference)
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

  nativeStatusSelect(): Locator {
    // Contract oracle: detect an accidental native-select regression by name/id.
    return this.page.locator('select[name="status"], select#status')
  }

  async uncheckColumn(label: string): Promise<void> {
    const box = this.columnCheckbox(label)
    if (await box.isChecked()) {
      await box.uncheck()
    }
  }
}
