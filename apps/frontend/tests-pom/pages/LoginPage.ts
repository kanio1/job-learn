import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class LoginPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/login')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('login-control')).toBeVisible()
  }

  async continueToKeycloak(): Promise<void> {
    await this.byTestId('login-control').click()
  }

  async useDifferentAccount(): Promise<void> {
    await this.byTestId('login-different-account').click()
  }

  ssoResumeNotice() {
    return this.byTestId('login-sso-resume-notice')
  }

  authRequiredSurface(): Locator { return this.byTestId('auth-required-surface') }
  keycloakHeading(): Locator { return this.page.getByRole('heading', { name: /sign in to your account/i }) }
  keycloakUsername(): Locator { return this.page.getByLabel(/username/i) }
  keycloakUsernameOrEmail(): Locator { return this.page.getByLabel('Username or email') }
  keycloakPassword(): Locator { return this.page.getByRole('textbox', { name: 'Password' }) }
  keycloakSubmit(): Locator { return this.page.getByRole('button', { name: /sign in/i }) }
  keycloakProtocolError(): Locator { return this.page.getByText(/invalid parameter|redirect.?uri|invalid request/i) }
}
