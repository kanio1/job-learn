import { uniqueLabUser } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'

test('platform admin sees the users table and page summary', async ({ app, api }) => {
  const listed = await requireApi(api).listUsers()
  expect(listed.status).toBe(200)

  await app.users.goto()
  await app.users.expectLoaded()
  await expect(app.page.getByText(/Page \d+ · .+ user\(s\) shown/i)).toBeVisible()
})

test('platform admin creates a unique user that appears in the table', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const user = uniqueLabUser(testInfo)

  await app.users.goto()
  await app.users.expectLoaded()
  await app.users.openCreate()
  await app.users.fillCreate({
    ...user,
    tenantId: 'TENANT_ALPHA',
    roleLabel: 'READ ONLY USER',
  })
  await app.users.submitCreate()

  await app.users.search(user.username)
  await expect(app.page).toHaveURL(url => url.searchParams.get('search') === user.username)
  await expect(app.users.rowByUsername(user.username)).toBeVisible()

  const listed = await client.listUsers({ search: user.username })
  expect(listed.status).toBe(200)
  expect(listed.body?.users?.some(entry => entry.username === user.username)).toBe(true)
})

test('platform admin disables a created user', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const user = uniqueLabUser(testInfo)
  const created = await client.createUser({
    ...user,
    tenantId: 'TENANT_ALPHA',
    roles: ['READ_ONLY_USER'],
  })
  expect(created.status).toBe(201)
  expect(created.body?.id).toBeTruthy()

  await app.users.goto()
  await app.users.expectLoaded()
  await app.users.search(user.username)
  await expect(app.page).toHaveURL(url => url.searchParams.get('search') === user.username)
  await expect(app.users.rowByUsername(user.username)).toBeVisible()
  await app.users.disableUser(user.username)
  await expect(app.page.getByRole('button', { name: `Enable ${user.username}` })).toBeVisible()

  const listed = await client.listUsers({ search: user.username, status: 'disabled' })
  expect(listed.status).toBe(200)
  const match = listed.body?.users?.find(entry => entry.username === user.username)
  expect(match?.enabled).toBe(false)
})

test('platform admin assigns an extra role on a created user', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const user = uniqueLabUser(testInfo)
  const created = await client.createUser({
    ...user,
    tenantId: 'TENANT_ALPHA',
    roles: ['READ_ONLY_USER'],
  })
  expect(created.status).toBe(201)

  await app.users.goto()
  await app.users.expectLoaded()
  await app.users.search(user.username)
  await expect(app.page).toHaveURL(url => url.searchParams.get('search') === user.username)
  await expect(app.users.rowByUsername(user.username)).toBeVisible()
  await app.users.openRoles(user.username)
  await app.users.addAssignedRole('SUPPORT AGENT')
  await app.users.saveRoles()
  await expect(app.users.rowByUsername(user.username)).toBeVisible()
  await expect(app.users.rowByUsername(user.username).getByText('SUPPORT AGENT', { exact: true })).toBeVisible()

  const listed = await client.listUsers({ search: user.username })
  expect(listed.status).toBe(200)
  const match = listed.body?.users?.find(entry => entry.username === user.username)
  expect(match?.roles).toEqual(expect.arrayContaining(['READ_ONLY_USER', 'SUPPORT_AGENT']))
})
