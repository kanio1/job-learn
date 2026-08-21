import { expect, type Locator, type Page } from '@playwright/test'

export class SavedViewsComponent {
  constructor(private readonly page: Page) {}

  openMenuButton(): Locator {
    return this.page.getByTestId('payment-views-open')
  }

  saveButton(): Locator {
    return this.page.getByTestId('payment-view-save')
  }

  nameInput(): Locator {
    return this.page.getByLabel('View name')
  }

  saveConfirmButton(): Locator {
    return this.page.getByTestId('payment-view-save-confirm')
  }

  item(name: string): Locator {
    return this.page.getByTestId('payment-views-list').getByRole('button', { name, exact: true })
  }

  defaultStar(name: string): Locator {
    return this.page.getByTestId('payment-views-list').getByRole('button', { name: `Set ${name} as default` })
  }

  async saveAs(name: string): Promise<void> {
    const saved = this.page.waitForResponse((response) => {
      const method = response.request().method()
      if (method !== 'POST' && method !== 'PUT') {
        return false
      }
      try {
        const pathname = new URL(response.url()).pathname
        return pathname === '/api/users/me/payment-views'
          || /^\/api\/users\/me\/payment-views\/[^/]+$/.test(pathname)
      }
      catch {
        return false
      }
    })
    await this.saveButton().click()
    await this.nameInput().fill(name)
    await this.saveConfirmButton().click()
    await saved
    await this.openMenuButton().click()
    await expect(this.item(name)).toBeVisible()
    await this.page.keyboard.press('Escape')
  }

  async open(name: string): Promise<void> {
    await this.openMenuButton().click()
    await this.item(name).click()
  }

  async setDefault(name: string): Promise<void> {
    const posted = this.page.waitForResponse((response) => {
      if (response.request().method() !== 'POST') {
        return false
      }
      try {
        return /\/api\/users\/me\/payment-views\/[^/]+\/default$/.test(new URL(response.url()).pathname)
      }
      catch {
        return false
      }
    })
    await this.openMenuButton().click()
    await this.defaultStar(name).click()
    expect((await posted).status()).toBe(200)
  }
}
