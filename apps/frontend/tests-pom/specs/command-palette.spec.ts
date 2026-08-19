import { test, expect } from '../fixtures'

test('Ctrl+K palette navigates to Error Lab', { tag: ['@a11y', '@ux'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()

  await test.step('open palette and match ARIA tree', async () => {
    await app.commandPalette.openWithKeyboard()
    await expect(app.commandPalette.dialog()).toMatchAriaSnapshot()
  })

  await test.step('type Error Lab and select', async () => {
    await app.commandPalette.search('Error Lab')
    await app.commandPalette.selectOption('Error Lab')
  })

  await expect(app.page).toHaveURL(/\/error-lab$/)
  await app.errorLab.expectLoaded()
})

const paletteDestinations = [
  { query: 'Checkout Lab', option: 'Checkout Lab', url: /\/admin\/checkout-lab$/, skipUnless: 'nav-link-checkout-lab' },
  { query: 'Merchant registry', option: 'Merchant registry', url: /\/admin\/merchants$/ },
  { query: 'Support', option: 'Support', url: /\/admin\/support$/ },
] as const

for (const destination of paletteDestinations) {
  test(`Ctrl+K palette navigates to ${destination.option}`, { tag: ['@ux'] }, async ({ app }) => {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    if ('skipUnless' in destination && destination.skipUnless) {
      if (await app.page.getByTestId(destination.skipUnless).count() === 0) {
        test.skip(true, `${destination.option} nav is hidden`)
      }
    }
    await app.commandPalette.openWithKeyboard()
    await app.commandPalette.search(destination.query)
    await app.commandPalette.selectOption(destination.option)
    await expect(app.page).toHaveURL(destination.url)
  })
}
