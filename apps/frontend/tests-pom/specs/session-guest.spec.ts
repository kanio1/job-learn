import { test, expect } from '../fixtures'

test('unauthenticated visit to merchants lands on login', { tag: ['@security'] }, async ({ app }) => {
  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login\?redirectTo=/)
  await app.login.expectLoaded()
})

test('unauthenticated visit to session lab lands on login', { tag: ['@security'] }, async ({ app }) => {
  await app.page.goto('/admin/session-lab')
  await expect(app.page).toHaveURL(/\/login\?redirectTo=/)
  await app.login.expectLoaded()
})
