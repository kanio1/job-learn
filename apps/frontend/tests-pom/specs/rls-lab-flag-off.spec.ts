import { test, expect, requireApi } from '../fixtures'

test('rls lab nav and page are absent when the public flag is off', async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expect(app.page.getByTestId('nav-link-rls-lab')).toHaveCount(0)

  await app.page.goto('/admin/rls-lab')
  await expect(app.page.getByTestId('rls-lab-items-table')).toHaveCount(0)
  await app.rlsLab.expectNotFound()
})

test('rls lab BFF returns 404 when the public flag is off', async ({ api }) => {
  const client = requireApi(api)
  const listed = await client.listRlsItems()
  expect(listed.status).toBe(404)
})
