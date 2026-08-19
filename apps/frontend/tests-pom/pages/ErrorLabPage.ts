import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { ProblemDetailsCard } from './components/ProblemDetailsCard'

export class ErrorLabPage extends BasePage {
  readonly problem: ProblemDetailsCard

  constructor(page: import('@playwright/test').Page) {
    super(page)
    this.problem = new ProblemDetailsCard(page)
  }

  async goto(): Promise<void> {
    await super.goto('/error-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByText('Learning surface')).toBeVisible()
  }

  triggerButton(status: 400 | 401 | 403 | 404 | 406 | 409 | 412 | 415 | 428 | 304) {
    return this.byTestId(`error-lab-trigger-${status}`)
  }

  async trigger(status: 400 | 401 | 403 | 404 | 406 | 409 | 412 | 415 | 428 | 304): Promise<void> {
    await this.assertNoDevErrorOverlay()
    const button = this.triggerButton(status)
    await button.scrollIntoViewIfNeeded()
    await expect(button).toBeEnabled()
    await button.click()
  }
}
