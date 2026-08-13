import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { ProblemDetailsCard } from './components/ProblemDetailsCard'

export class SupportPage extends BasePage {
  readonly problem: ProblemDetailsCard

  constructor(page: import('@playwright/test').Page) {
    super(page)
    this.problem = new ProblemDetailsCard(page)
  }

  async goto(): Promise<void> {
    await super.goto('/admin/support')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('support-search-button')).toBeVisible()
  }

  async search(merchantId: string, clientOrderReference?: string): Promise<void> {
    await this.byTestId('support-search-merchant-id').fill(merchantId)
    if (clientOrderReference) {
      await this.byTestId('support-search-client-ref').fill(clientOrderReference)
    }
    await this.byTestId('support-search-button').click()
  }

  async expectProblem(): Promise<void> {
    await expect(this.byTestId('error-state')).toBeVisible()
    await this.problem.expectVisible()
  }
}
