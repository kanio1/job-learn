import { type Locator, type Page } from '@playwright/test'

export class SupportCaseCard {
  constructor(
    private readonly page: Page,
    readonly caseId: string,
  ) {}

  root(): Locator {
    return this.page.getByTestId(`support-card-${this.caseId}`)
  }

  select(): Locator {
    return this.page.getByTestId(`support-card-select-${this.caseId}`)
  }

  async moveTo(status: string): Promise<void> {
    await this.root().getByRole('button', { name: /^Move / }).click()
    await this.page.getByRole('menuitem', { name: `Move to ${status}` }).click()
  }

  async dragToColumn(column: Locator): Promise<void> {
    const drop = column.locator('[data-testid^="kanban-drop-"]')
    await this.root().scrollIntoViewIfNeeded()
    await drop.scrollIntoViewIfNeeded()
    await this.root().dragTo(drop, { targetPosition: { x: 24, y: 12 } })
  }
}
