import { expect, type Locator, type Page } from '@playwright/test'

export abstract class BasePage {
  constructor(protected readonly page: Page) {}

  protected byTestId(testId: string): Locator {
    return this.page.getByTestId(testId)
  }

  async goto(path: string): Promise<void> {
    await this.page.goto(path)
    await this.assertNoDevErrorOverlay()
  }

  /**
   * vue-tsc / vite-plugin-checker overlay intercepts pointer events. Tests must
   * not click it away — that hides a real typecheck failure. RLS lab uses ofetch
   * instead of $fetch typed routes to avoid generating the overlay in the first place.
   */
  protected async assertNoDevErrorOverlay(): Promise<void> {
    const overlayCount = await this.page.locator('vite-plugin-checker-error-overlay').count()
    if (overlayCount > 0) {
      throw new Error(
        'vite-plugin-checker-error-overlay is present. Fix the typecheck error; do not dismiss the overlay from POM.',
      )
    }
  }

  abstract expectLoaded(): Promise<void>

  async expectHeading(name: string | RegExp): Promise<void> {
    await expect(this.page.getByRole('heading', { name })).toBeVisible()
  }
}
