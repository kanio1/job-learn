import type { Page } from '@playwright/test'

/** Global dashboard idle lock (`SessionLabIdleLock` in the dashboard layout). */
export class IdleOverlay {
  constructor(private readonly page: Page) {}

  lock() {
    return this.page.getByTestId('session-lab-idle-lock')
  }

  async unlock(): Promise<void> {
    await this.page.getByTestId('session-lab-idle-unlock').click()
  }
}
