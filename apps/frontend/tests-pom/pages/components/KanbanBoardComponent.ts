import { expect, type Locator, type Page } from '@playwright/test'
import { SupportCaseCard } from './SupportCaseCard'

export class KanbanBoardComponent {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('support-kanban')
  }

  column(status: string): Locator {
    return this.page.getByTestId(`kanban-column-${status}`)
  }

  dropZone(status: string): Locator {
    return this.page.getByTestId(`kanban-drop-${status}`)
  }

  card(caseId: string): SupportCaseCard {
    return new SupportCaseCard(this.page, caseId)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.root()).toBeVisible()
    await expect(this.column('NEW')).toBeVisible()
    await expect(this.column('IN_PROGRESS')).toBeVisible()
    await expect(this.column('WAITING')).toBeVisible()
    await expect(this.column('RESOLVED')).toBeVisible()
  }

  async expectCardIn(caseId: string, status: string): Promise<void> {
    await expect(this.column(status).getByTestId(`support-card-${caseId}`)).toBeVisible()
  }
}
