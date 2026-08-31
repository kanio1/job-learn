import { uniqueMerchantReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { waitForBffResponse } from '../utils/wait-bff'
import { z } from 'zod'

const searchResponseSchema = z.object({
  merchants: z.array(z.object({ merchantReference: z.string().optional() }).passthrough()).optional(),
}).passthrough()

test('PW-OPS-E2E-202 last in-flight search q wins; first merchant absent', async ({
  app,
  api,
  page,
}, testInfo) => {
  const client = api
  const first = uniqueMerchantReference(testInfo)
  const second = uniqueMerchantReference(testInfo)
  expect((await client.merchants.create(first, `First ${first}`)).status).toBe(201)
  expect((await client.merchants.create(second, `Second ${second}`)).status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.commandPalette.openWithKeyboard()
  const firstSearch = waitForBffResponse(page, { method: 'GET', pathExact: '/api/search', queryExact: { q: first } })
  const secondSearch = waitForBffResponse(page, { method: 'GET', pathExact: '/api/search', queryExact: { q: second } })
  await app.commandPalette.fillSearch(first)
  await app.commandPalette.fillSearch(second)
  const last = await secondSearch
  expect(new URL(last.url()).searchParams.get('q')).toBe(second)
  await firstSearch.catch(() => undefined)
  const body = searchResponseSchema.parse(await last.json())
  expect(body.merchants?.some(row => row.merchantReference === second)).toBe(true)
  await expect(app.commandPalette.dialog().getByRole('option', { name: second })).toBeVisible()
  await expect(app.commandPalette.dialog().getByRole('option', { name: first })).toHaveCount(0)
})
