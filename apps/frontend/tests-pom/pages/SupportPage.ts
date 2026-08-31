import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { ProblemDetailsCard } from './components/ProblemDetailsCard'
import { KanbanBoardComponent } from './components/KanbanBoardComponent'

export class SupportPage extends BasePage {
  readonly problem: ProblemDetailsCard
  readonly board: KanbanBoardComponent

  constructor(page: Page) {
    super(page)
    this.problem = new ProblemDetailsCard(page)
    this.board = new KanbanBoardComponent(page)
  }

  override async goto(): Promise<void> {
    await super.goto('/admin/support')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('support-search-button')).toBeVisible()
  }

  async openWorkQueue(): Promise<void> {
    await this.page.getByRole('tab', { name: 'Work Queue' }).click()
    await this.board.expectLoaded()
  }

  queueTab(): Locator { return this.page.getByRole('tab', { name: 'Kolejka' }) }
  resultsTable(): Locator { return this.page.getByRole('table', { name: 'Support search results' }) }
  errorState(): Locator { return this.byTestId('error-state') }
  errorToast(): Locator { return this.page.getByRole('alert') }

  async selectCases(caseIds: readonly string[]): Promise<void> {
    for (const caseId of caseIds) {
      await this.board.card(caseId).select().click()
    }
  }

  async search(merchantId: string, clientOrderReference?: string): Promise<void> {
    await this.byTestId('support-search-merchant-id').fill(merchantId)
    if (clientOrderReference) {
      await this.byTestId('support-search-client-ref').fill(clientOrderReference)
    }
    await this.byTestId('support-search-button').click()
  }

  results(): Locator { return this.byTestId('support-search-results') }
  resultsSummary(): Locator { return this.page.getByText(/^Results \([1-9]/) }

  /** Open the bulk-assign dialog (business assertions stay in the spec). */
  async openBulkAssign(): Promise<void> {
    await this.byTestId('support-bulk-assign').click()
  }

  async assignTo(subject: string): Promise<void> {
    await this.byTestId('bulk-assign-assignee').fill(subject)
    await this.submitBulkAssign()
  }

  async submitBulkAssign(): Promise<void> {
    await this.byTestId('bulk-assign-submit').click()
  }

  async retryFailed(): Promise<void> {
    await this.byTestId('bulk-retry-failed').click()
  }

  bulkResult(): Locator {
    return this.byTestId('bulk-assign-result')
  }

  bulkProgress(): Locator {
    return this.byTestId('bulk-assign-progress')
  }

  bulkSuccessCount(): Locator {
    return this.byTestId('bulk-success-count')
  }

  bulkFailureRows(): Locator {
    // Each server-created failure row has a dynamic test id; the collection is the oracle.
    return this.page.locator('[data-testid^="bulk-failure-row-"]')
  }
}
