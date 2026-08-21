import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { ProblemDetailsCard } from './components/ProblemDetailsCard'
import { KanbanBoardComponent } from './components/KanbanBoardComponent'

export class SupportPage extends BasePage {
  readonly problem: ProblemDetailsCard
  readonly board: KanbanBoardComponent

  constructor(page: import('@playwright/test').Page) {
    super(page)
    this.problem = new ProblemDetailsCard(page)
    this.board = new KanbanBoardComponent(page)
  }

  async goto(): Promise<void> {
    await super.goto('/admin/support')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('support-search-button')).toBeVisible()
  }

  async openWorkQueue(): Promise<void> {
    await this.page.getByRole('tab', { name: 'Work Queue' }).click()
    await this.board.expectLoaded()
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

  async expectResults(): Promise<void> {
    await expect(this.byTestId('support-search-results')).toBeVisible()
    await expect(this.page.getByText(/^Results \([1-9]/)).toBeVisible()
    await expect(this.page.getByRole('table', { name: 'Support search results' })).toBeVisible()
  }
}
