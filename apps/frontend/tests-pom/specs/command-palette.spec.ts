import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'

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

function waitForSearch(page: import('@playwright/test').Page, q: string) {
  return page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    try {
      const url = new URL(response.url())
      return url.pathname === '/api/search' && url.searchParams.get('q') === q
    }
    catch {
      return false
    }
  })
}

test.describe('Command palette entity search', () => {
  test('PW-M360-E2E-110 Ctrl+K unique merchant option and GET search 200', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Palette ${reference}`)
    expectStatus(created, 201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.commandPalette.openWithKeyboard()
    const search = waitForSearch(page, reference)
    await app.commandPalette.search(reference)
    const response = await search
    expect(response.status()).toBe(200)
    await expect(app.commandPalette.dialog().getByRole('option', { name: reference })).toBeVisible()
  })

  test('PW-M360-E2E-111 select merchant opens 360 for that id', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Jump ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.commandPalette.openWithKeyboard()
    const search = waitForSearch(page, reference)
    await app.commandPalette.search(reference)
    expect((await search).status()).toBe(200)
    await app.commandPalette.dialog().getByRole('option', { name: reference }).first().click()
    await expect(app.page).toHaveURL(new RegExp(`merchantId=${merchantId}`))
    await app.merchantSlideover.expectOpen()
  })

  test('PW-M360-E2E-112 last search body contains only q hit', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.createMerchant(reference, `Last ${reference}`)).status).toBe(201)
    expect((await client.createMerchant(uniqueMerchantReference(testInfo), 'Other palette merchant')).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.commandPalette.openWithKeyboard()
    const search = waitForSearch(page, reference)
    await app.commandPalette.search(reference)
    const response = await search
    expect(response.status()).toBe(200)
    const body = await response.json()
    expect(body.merchants).toEqual([
      expect.objectContaining({ merchantReference: reference }),
    ])
  })

  test('PW-M360-API-051 search BFF 200 Zod-shaped body', async ({ api }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.createMerchant(reference, `ApiSearch ${reference}`)).status).toBe(201)
    const searched = await client.searchEntities(reference)
    expectStatus(searched, 200)
    expect(searched.body.merchants?.some(row => row.merchantReference === reference)).toBe(true)
  })
})
