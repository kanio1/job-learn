import { type Page } from '@playwright/test'

export class UserMenu {
  constructor(private readonly page: Page) {}

  async signOut(): Promise<void> {
    await this.page.getByTestId('logout-control').click()
    await this.page.getByRole('menuitem', { name: 'Sign out' }).click()
  }
}
