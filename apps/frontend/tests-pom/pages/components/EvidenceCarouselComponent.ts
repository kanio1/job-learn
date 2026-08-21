import { expect, type Locator, type Page } from '@playwright/test'

export class EvidenceCarouselComponent {
  constructor(private readonly page: Page) {}

  root(): Locator {
    return this.page.getByTestId('evidence-carousel')
  }

  slide(index: number): Locator {
    return this.page.getByTestId(`evidence-slide-${index}`)
  }

  region(): Locator {
    return this.root().getByRole('region')
  }

  next(): Locator {
    return this.root().getByRole('button', { name: 'Next' })
  }

  prev(): Locator {
    return this.root().getByRole('button', { name: 'Prev' })
  }

  errorSlide(): Locator {
    return this.page.getByTestId('evidence-error-slide')
  }

  async expectOpen(): Promise<void> {
    await expect(this.root()).toBeVisible()
  }

  async expectIndex(index: number): Promise<void> {
    await expect(this.root()).toHaveAttribute('data-active-index', String(index))
  }

  async goNext(): Promise<void> {
    await this.next().click()
  }
}
