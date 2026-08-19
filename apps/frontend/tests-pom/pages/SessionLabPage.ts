import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

export class SessionLabPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/session-lab')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('session-lab-js-cookies')).toBeVisible()
  }

  async endOidc(): Promise<void> {
    await this.byTestId('session-lab-end-oidc').click({ noWaitAfter: true })
  }

  async csrfOk(): Promise<void> {
    await this.byTestId('session-lab-csrf-ok').click()
  }

  async csrfFail(): Promise<void> {
    await this.byTestId('session-lab-csrf-fail').click()
  }

  deviceList() {
    return this.byTestId('session-lab-device-list')
  }

  revokeButton(deviceId: string) {
    return this.byTestId(`session-lab-revoke-${deviceId}`)
  }

  async revoke(deviceId: string): Promise<void> {
    await this.revokeButton(deviceId).click()
  }
}
