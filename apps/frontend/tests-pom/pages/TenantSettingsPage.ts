import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { RuleConfiguratorComponent } from './components/RuleConfiguratorComponent'

export class TenantSettingsPage extends BasePage {
  readonly rules: RuleConfiguratorComponent

  constructor(page: Page) {
    super(page)
    this.rules = new RuleConfiguratorComponent(page)
  }

  override async goto(): Promise<void> {
    await super.goto('/admin/tenant/settings')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('tenant-settings-form')).toBeVisible()
    await this.rules.expectOpen()
  }

  async fillContactEmail(email: string): Promise<void> {
    await this.byTestId('tenant-settings-contact-email').fill(email)
  }

  saveButton() {
    return this.byTestId('tenant-settings-save')
  }

  savedNotice(): Locator { return this.page.getByText('Settings saved', { exact: true }) }

  async save(): Promise<void> {
    await this.saveButton().click()
  }
}
