import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import {
  expectNoJwtInStorageStateFile,
  expectNoTokenInBrowserStorage,
  expectSessionCookieHttpOnly,
} from '../utils/storage-safety'

test('logout returns to login and blocks admin again', { tag: ['@security'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.userMenu.signOut()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()

  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('session cookie is HttpOnly and storageState has no JWT', { tag: ['@security'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expectSessionCookieHttpOnly(app.page)
  await expectNoTokenInBrowserStorage(app.page)
  expectNoJwtInStorageStateFile(pomAuthFiles.platformAdmin)
})
