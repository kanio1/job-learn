import { test, expect } from '../fixtures'

test('rls lab nav and page are absent when the public flag is off', async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expect(app.sidebar.rlsLab()).toHaveCount(0)

  await app.page.goto('/admin/rls-lab')
  await expect(app.rlsLab.itemsTable()).toHaveCount(0)
  await app.rlsLab.expectNotFound()
})

test('rls lab BFF returns 404 when the public flag is off', async ({ api }) => {
  const client = api
  const listed = await client.labs.listRlsItems()
  expect(listed.status).toBe(404)
})
