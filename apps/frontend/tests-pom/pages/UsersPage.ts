import { expect, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class UsersPage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/users')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('users-table')).toBeVisible()
  }

  async expectForbidden(): Promise<void> {
    await expect(this.byTestId('forbidden-state')).toBeVisible()
  }

  async openCreate(): Promise<void> {
    await this.byTestId('create-user-button').click()
    await expect(this.byTestId('create-user-form')).toBeVisible()
  }

  async fillCreate(input: {
    username: string
    email: string
    temporaryPassword: string
    tenantId: string
    roleLabel: string
  }): Promise<void> {
    const form = this.byTestId('create-user-form')
    await form.getByLabel('Username').fill(input.username)
    await form.getByLabel('Email').fill(input.email)
    await form.getByLabel('Temporary password').fill(input.temporaryPassword)
    await form.getByLabel('Tenant reference').fill(input.tenantId)
    await form.getByTestId('role-assignment-select').click()
    await this.page.getByRole('option', { name: input.roleLabel }).click()
    await this.page.keyboard.press('Escape')
  }

  async submitCreate(): Promise<void> {
    await this.byTestId('create-user-form').getByRole('button', { name: 'Create user' }).click()
    await expect(this.byTestId('create-user-form')).toBeHidden()
  }

  async search(username: string): Promise<void> {
    await this.page.getByLabel('Search users').fill(username)
    await this.page.getByRole('button', { name: 'Apply filters' }).click()
  }

  async filterByRole(label: string): Promise<void> {
    await this.page.getByLabel('Filter by role').click()
    await this.page.getByRole('option', { name: label, exact: true }).click()
    await this.page.keyboard.press('Escape')
    await this.page.getByRole('button', { name: 'Apply filters' }).click()
  }

  async filterByStatus(label: string): Promise<void> {
    await this.page.getByLabel('Filter by status').click()
    await this.page.getByRole('option', { name: label, exact: true }).click()
    await this.page.keyboard.press('Escape')
    await this.page.getByRole('button', { name: 'Apply filters' }).click()
  }

  rowByUsername(username: string): Locator {
    return this.page.getByRole('row').filter({ hasText: username })
  }

  async disableUser(username: string): Promise<void> {
    await this.page.getByRole('button', { name: `Disable ${username}` }).click()
  }

  async openRoles(username: string): Promise<void> {
    await this.page.getByRole('button', { name: `Manage roles for ${username}` }).click()
    await expect(this.byTestId('edit-user-drawer')).toBeVisible()
  }

  async addAssignedRole(roleLabel: string): Promise<void> {
    const drawer = this.byTestId('edit-user-drawer')
    await drawer.getByTestId('role-assignment-select').click()
    await this.page.getByRole('option', { name: roleLabel }).click()
    await this.page.keyboard.press('Escape')
  }

  async saveRoles(): Promise<void> {
    await this.byTestId('edit-user-drawer').getByRole('button', { name: 'Save roles' }).click()
    await expect(this.byTestId('edit-user-drawer')).toBeHidden()
  }
}
