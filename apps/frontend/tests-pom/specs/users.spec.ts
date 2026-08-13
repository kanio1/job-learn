import { test, expect } from '../fixtures'

test('platform admin sees the users table and page summary', async ({ app, api }) => {
  const listed = await api.listUsers()
  expect(listed.status).toBe(200)

  await app.users.goto()
  await app.users.expectLoaded()
  await expect(app.page.getByText(/Page \d+ · .+ user\(s\) shown/i)).toBeVisible()
})
