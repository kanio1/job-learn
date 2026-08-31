import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { ProblemDetailsCard } from './components/ProblemDetailsCard'

export class ErrorLabPage extends BasePage {
  readonly problem: ProblemDetailsCard

  constructor(page: Page) {
    super(page)
    this.problem = new ProblemDetailsCard(page)
  }

  override async goto(): Promise<void> {
    await super.goto('/error-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByText('Learning surface')).toBeVisible()
  }

  triggerButton(status: 400 | 401 | 403 | 404 | 406 | 409 | 412 | 415 | 428 | 429 | 304): Locator {
    return this.byTestId(`error-lab-trigger-${status}`)
  }

  pspRedirectTrigger(): Locator { return this.byTestId('psp-redirect-trigger') }

  async trigger(status: 400 | 401 | 403 | 404 | 406 | 409 | 412 | 415 | 428 | 429 | 304): Promise<void> {
    await this.assertNoDevErrorOverlay()
    const button = this.triggerButton(status)
    await button.scrollIntoViewIfNeeded()
    await expect(button).toBeEnabled()
    await button.click()
  }
}
