import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class EventLabPage extends BasePage {
  override async goto(search = ''): Promise<void> {
    await super.goto(search ? `/admin/event-lab?targetId=${encodeURIComponent(search)}` : '/admin/event-lab')
  }

  async gotoDetail(id: string): Promise<void> {
    await super.goto(`/admin/event-lab?id=${encodeURIComponent(id)}`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.heading()).toBeVisible()
  }

  heading(): Locator { return this.page.getByRole('heading', { name: /Event Lab/i }) }
  loading(): Locator { return this.byTestId('event-lab-loading') }
  filteredEmpty(): Locator { return this.byTestId('event-lab-filtered-empty') }
  forbidden(): Locator { return this.byTestId('event-lab-forbidden') }
  error(): Locator { return this.byTestId('event-lab-error') }
  notFound(): Locator { return this.byTestId('event-lab-not-found') }
  table(): Locator { return this.byTestId('event-lab-table') }
  empty(): Locator { return this.byTestId('event-lab-empty') }
  settledListState(): Locator { return this.table().or(this.filteredEmpty()).or(this.empty()) }
  duplicateConfirmation(): Locator { return this.byTestId('confirm-inject-duplicate') }
  poisonConfirmation(): Locator { return this.byTestId('confirm-inject-poison') }
  dltBanner(): Locator { return this.byTestId('event-lab-dlt-banner') }
  eventRow(targetId: string): Locator { return this.table().getByText(targetId, { exact: true }) }
  payloadColumn(): Locator { return this.table().getByRole('columnheader', { name: /payload/i }) }

  async expectForbidden(): Promise<void> {
    await expect(this.forbidden()).toBeVisible()
  }

  async search(value: string): Promise<void> {
    await this.byTestId('event-lab-search').fill(value)
  }

  async injectDuplicate(): Promise<void> {
    await this.byTestId('event-lab-inject-duplicate').click()
    await expect(this.page.getByTestId('confirm-inject-duplicate')).toBeVisible()
  }

  async confirmDuplicate(): Promise<void> {
    await this.byTestId('confirm-inject-duplicate').click()
  }

  async dismissDuplicate(): Promise<void> {
    await this.page.getByRole('button', { name: 'Cancel' }).click()
  }

  async injectPoison(): Promise<void> {
    await this.byTestId('event-lab-inject-poison').click()
    await expect(this.poisonConfirmation()).toBeVisible()
  }

  async confirmPoison(): Promise<void> {
    await this.byTestId('confirm-inject-poison').click()
  }

  async rowCount(): Promise<number> {
    return (await this.table().getByRole('row').count()) - 1
  }
}
