import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class EventLabPage extends BasePage {
  override async goto(search = ''): Promise<void> {
    await super.goto(search ? `/admin/event-lab?targetId=${encodeURIComponent(search)}` : '/admin/event-lab')
  }

  async gotoDetail(id: string): Promise<void> {
    await super.goto(`/admin/event-lab?id=${encodeURIComponent(id)}`)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByRole('heading', { name: /Event Lab/i }).first()).toBeVisible()
  }

  async expectLoading(): Promise<void> {
    await expect(this.byTestId('event-lab-loading')).toBeVisible()
  }

  async expectEmpty(): Promise<void> {
    await expect(this.byTestId('event-lab-empty')).toBeVisible()
  }

  async expectFilteredEmpty(): Promise<void> {
    await expect(this.byTestId('event-lab-filtered-empty')).toBeVisible()
  }

  async expectForbidden(): Promise<void> {
    await expect(this.byTestId('event-lab-forbidden')).toBeVisible()
  }

  async expectError(): Promise<void> {
    await expect(this.byTestId('event-lab-error')).toBeVisible()
  }

  override async expectNotFound(): Promise<void> {
    await expect(this.byTestId('event-lab-not-found')).toBeVisible()
  }

  async expectRowVisible(targetId: string): Promise<void> {
    await expect(this.page.getByTestId('event-lab-table').getByText(targetId).first()).toBeVisible()
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
    await expect(this.page.getByTestId('confirm-inject-poison')).toBeVisible()
  }

  async confirmPoison(): Promise<void> {
    await this.byTestId('confirm-inject-poison').click()
  }

  async expectDltBanner(): Promise<void> {
    await expect(this.byTestId('event-lab-dlt-banner')).toBeVisible()
  }

  async expectDeliveryPending(): Promise<void> {
    await expect(this.byTestId('eventlab-delivery-pending')).toBeVisible()
  }

  async expectDeliveryProcessed(): Promise<void> {
    await expect(this.byTestId('eventlab-delivery-processed')).toBeVisible()
  }

  async expectDeliveryDead(): Promise<void> {
    await expect(this.byTestId('eventlab-delivery-dlt-banner')).toBeVisible()
  }

  async rowCount(): Promise<number> {
    return this.page.getByTestId('event-lab-table').locator('tbody tr').count()
  }

  async hasNoPayloadColumn(): Promise<boolean> {
    const headers = this.page.getByTestId('event-lab-table').locator('th')
    const count = await headers.count()
    for (let i = 0; i < count; i++) {
      const text = (await headers.nth(i).textContent()) || ''
      if (/payload/i.test(text)) return false
    }
    return true
  }
}
