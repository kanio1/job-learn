import { type Locator, type Page } from '@playwright/test'
import { completeKeycloakEndSession } from '../../utils/keycloak-oidc'

export class UserMenu {
  constructor(private readonly page: Page) {}

  control(): Locator { return this.page.getByTestId('logout-control') }
  signOutOption(): Locator { return this.page.getByRole('menuitem', { name: 'Sign out', exact: true }) }

  async signOut(): Promise<void> {
    const landed = this.page.waitForURL(/\/protocol\/openid-connect\/logout|\/login/, { timeout: 15_000 })
    await this.control().click()
    await this.signOutOption().click()
    await landed
    await completeKeycloakEndSession(this.page)
  }

  async signOutOfDashboardOnly(): Promise<void> {
    await this.page.getByTestId('logout-control').click()
    await this.page.getByRole('menuitem', { name: 'Sign out of dashboard only' }).click()
  }
}
