import { test, expect } from '../fixtures'

test('Ctrl+K palette navigates to Error Lab', { tag: ['@a11y', '@ux'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()

  await test.step('open palette and match ARIA tree', async () => {
    await app.commandPalette.openWithKeyboard()
    await expect(app.commandPalette.dialog()).toMatchAriaSnapshot()
  })

  await test.step('type Error Lab and select with keyboard', async () => {
    await app.commandPalette.search('Error Lab')
    await expect(app.page.getByRole('option').first()).toBeVisible()
    await app.page.keyboard.press('ArrowDown')
    await app.page.keyboard.press('Enter')
  })

  await expect(app.page).toHaveURL(/\/error-lab$/)
  await app.errorLab.expectLoaded()
})
