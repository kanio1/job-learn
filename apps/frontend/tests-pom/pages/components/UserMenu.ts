import { type Page } from '@playwright/test'
import { completeKeycloakEndSession } from '../../utils/keycloak-oidc'

export class UserMenu {
  constructor(private readonly page: Page) {}

  async signOut(): Promise<void> {
    const landed = this.page.waitForURL(/\/protocol\/openid-connect\/logout|\/login/, { timeout: 15_000 })
    await this.page.getByTestId('logout-control').click()
    await this.page.getByRole('menuitem', { name: 'Sign out', exact: true }).click()
    await landed
    await completeKeycloakEndSession(this.page)
  }

  async signOutOfDashboardOnly(): Promise<void> {
    await this.page.getByTestId('logout-control').click()
    await this.page.getByRole('menuitem', { name: 'Sign out of dashboard only' }).click()
  }
}
