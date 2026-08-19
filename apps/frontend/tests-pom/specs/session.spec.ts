import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import {
  expectNoJwtInStorageStateFile,
  expectNoTokenInBrowserStorage,
  expectSessionCookieHttpOnly,
} from '../utils/storage-safety'

test.use({ storageState: pomAuthFiles.platformAdminSession })

test('two contexts sharing storageState can revoke a device', { tag: ['@security'] }, async ({ browser }) => {
  const first = await browser.newContext({ storageState: pomAuthFiles.platformAdminSession })
  const second = await browser.newContext({ storageState: pomAuthFiles.platformAdminSession })
  const page1 = await first.newPage()
  const page2 = await second.newPage()
  try {
    await page1.goto('/admin/session-lab')
    await page2.goto('/admin/session-lab')
    await expect(page1.getByTestId('session-lab-device-list')).toBeVisible()
    await expect(page2.getByTestId('session-lab-device-list')).toBeVisible()
    const revoke = page1.getByRole('button', { name: 'Revoke' }).first()
    await expect(revoke).toBeVisible()
    await revoke.click()
    await expect(page1.getByTestId('session-lab-device-list')).toBeVisible()
  }
  finally {
    await first.close()
    await second.close()
  }
})

test('logout returns to login and blocks admin again', { tag: ['@security', '@serial'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.userMenu.signOut()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()

  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('second logout from login stays on login (EG-W2-11)', { tag: ['@security'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.userMenu.signOut()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
  await expect(app.page.getByTestId('logout-control')).toHaveCount(0)
  await app.login.goto()
  await app.login.expectLoaded()
  await expect(app.page.getByTestId('logout-control')).toHaveCount(0)
  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('session cookie is HttpOnly and storageState has no JWT', { tag: ['@security'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expectSessionCookieHttpOnly(app.page)
  await expectNoTokenInBrowserStorage(app.page)
  expectNoJwtInStorageStateFile(pomAuthFiles.platformAdminSession)
})
