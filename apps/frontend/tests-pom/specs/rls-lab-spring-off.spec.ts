import { test, expect } from '../fixtures'

test('RLS hub loads while Spring RLS API is 404', async ({ app, api }) => {
  test.skip(
    process.env.PLAYWRIGHT_RLS_SPRING_OFF !== '1',
    'Set PLAYWRIGHT_RLS_SPRING_OFF=1 and start Spring with RLS_LAB_ENABLED=false',
  )
  const client = api
  await app.page.goto('/admin/rls-lab')
  await expect(app.rlsLab.item('Java WHERE is not RLS')).toBeVisible()
  await expect(app.sidebar.rlsLab()).toBeVisible()
  const listed = await client.labs.listRlsItems()
  expect(listed.status).toBe(404)
})
