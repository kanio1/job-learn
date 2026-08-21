import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'

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

test('PW-OPS-E2E-202 last in-flight search q wins; first merchant absent', async ({
  app,
  api,
  page,
}, testInfo) => {
  const client = requireApi(api)
  const first = uniqueMerchantReference(testInfo)
  const second = uniqueMerchantReference(testInfo)
  expect((await client.createMerchant(first, `First ${first}`)).status).toBe(201)
  expect((await client.createMerchant(second, `Second ${second}`)).status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.commandPalette.openWithKeyboard()
  const firstSearch = waitForSearch(page, first)
  const secondSearch = waitForSearch(page, second)
  await app.commandPalette.fillSearch(first)
  await app.commandPalette.fillSearch(second)
  const last = await secondSearch
  expect(new URL(last.url()).searchParams.get('q')).toBe(second)
  await firstSearch.catch(() => undefined)
  const body = await last.json() as { merchants?: Array<{ merchantReference?: string }> }
  expect(body.merchants?.some(row => row.merchantReference === second)).toBe(true)
  await expect(app.commandPalette.dialog().getByRole('option', { name: second })).toBeVisible()
  await expect(app.commandPalette.dialog().getByRole('option', { name: first })).toHaveCount(0)
})
