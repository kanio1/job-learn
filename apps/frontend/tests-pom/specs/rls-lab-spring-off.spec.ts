import { test, expect, requireApi } from '../fixtures'

test('RLS hub loads while Spring RLS API is 404', async ({ app, api }) => {
  test.skip(
    process.env.PLAYWRIGHT_RLS_SPRING_OFF !== '1',
    'Set PLAYWRIGHT_RLS_SPRING_OFF=1 and start Spring with RLS_LAB_ENABLED=false',
  )
  const client = requireApi(api)
  await app.page.goto('/admin/rls-lab')
  await expect(app.page.getByText('Java WHERE is not RLS')).toBeVisible()
  await expect(app.page.getByTestId('nav-link-rls-lab')).toBeVisible()
  const listed = await client.listRlsItems()
  expect(listed.status).toBe(404)
})
