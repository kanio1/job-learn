import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'
import { OpsFeedComponent } from './components/OpsFeedComponent'
import { NotificationCenterComponent } from './components/NotificationCenterComponent'

export class OverviewPage extends BasePage {
  readonly opsFeed: OpsFeedComponent
  readonly notifications: NotificationCenterComponent

  constructor(page: import('@playwright/test').Page) {
    super(page)
    this.opsFeed = new OpsFeedComponent(page)
    this.notifications = new NotificationCenterComponent(page)
  }

  override async goto(): Promise<void> {
    await super.goto('/')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByTestId('nav-link-overview')).toBeVisible()
    await this.opsFeed.expectLoaded()
  }

  summary(): Locator { return this.page.getByRole('region', { name: 'Summary' }) }
  summaryHeading(): Locator { return this.page.getByRole('heading', { name: 'Platform Summary' }) }
  merchantForbiddenHint(): Locator { return this.page.getByTestId('overview-merchant-forbidden-hint') }
}
