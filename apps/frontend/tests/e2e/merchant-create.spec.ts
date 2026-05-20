import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession, mockMerchantApi, uniqueReference } from './merchant-support'

test('creates a merchant from the empty registry', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [])

  await page.goto('/admin/merchants')

  await expect(page.getByText('No merchants have been registered yet')).toBeVisible()
  await page.getByRole('button', { name: 'Create merchant' }).click()

  const reference = uniqueReference('CREATE')
  await page.getByLabel('Merchant reference').fill(reference)
  await page.getByLabel('Display name').fill('Created Merchant')
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  await expect(page.getByText('Merchant created', { exact: true })).toBeVisible()
  await expect(page.getByText(reference)).toBeVisible()
  await expect(page.getByText('DRAFT', { exact: true })).toBeVisible()
})

test('shows create validation and duplicate feedback', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockMerchantApi(page, [])

  await page.goto('/admin/merchants')
  await page.getByRole('button', { name: 'Create merchant' }).click()
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  await expect(page.getByText('Reference must be at least 3 characters')).toBeVisible()

  const reference = uniqueReference('DUP')
  await page.getByLabel('Merchant reference').fill(reference)
  await page.getByLabel('Display name').fill('Duplicate Merchant')
  await page.getByRole('button', { name: 'Create', exact: true }).click()
  await expect(page.getByText('Merchant created', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Create merchant' }).click()
  await page.getByLabel('Merchant reference').fill(reference)
  await page.getByLabel('Display name').fill('Duplicate Merchant')
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  await expect(page.getByText('A merchant with this reference already exists')).toBeVisible()
})
