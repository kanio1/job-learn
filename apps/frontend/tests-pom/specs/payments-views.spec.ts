import { merchantManagerAccountForWorker, POM_WORKER_COUNT } from '../auth/accounts'
import { type BffClient, expectStatus } from '../api/bff-client'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { z } from 'zod'

const LARGE_EUR_VIEW = 'Large EUR captured'
const sessionSchema = z.object({ user: z.object({ id: z.string().optional() }).optional() }).passthrough()
const savedViewsSchema = z.array(z.object({
  name: z.string().optional(),
  filters: z.record(z.string(), z.unknown()).optional(),
}).passthrough())

// Each example mutates the persistent views for its authenticated user. Keep
// this file on one worker so teardown cannot delete another example's default
// view; other POM files remain fully parallel across the four worker worlds.
test.describe.configure({ mode: 'serial' })

async function clearPaymentViews(client: BffClient): Promise<void> {
  const listed = await client.identity.listPaymentViews()
  expectStatus(listed, 200)
  await Promise.all((listed.body.content ?? []).map(view => client.identity.deletePaymentView(view.id)))
}

test.beforeEach(async ({ api }) => {
  await clearPaymentViews(api)
})

test.afterEach(async ({ api }) => {
  await clearPaymentViews(api)
})

async function sessionUserId(page: import('@playwright/test').Page): Promise<string> {
  const session = await page.evaluate(async () => {
    const response = await fetch('/api/_auth/session')
    if (!response.ok) {
      throw new Error(`session ${response.status}`)
    }
    return await response.json()
  })
  const id = sessionSchema.parse(session).user?.id
  ok(id, 'sanitized session user id (JWT sub) must be present')
  return id
}

test('PW-OPS-E2E-140 save payment view then reload restores filters', async ({
  app,
  api,
  ownedMerchantId,
}, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'VIEW')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 15000, currency: 'EUR', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'VIEW'),
  )
  expectStatus(created, 201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(LARGE_EUR_VIEW)
  await app.payments.filters.clear()
  await expect(app.page).not.toHaveURL(/status=CAPTURED/)

  await app.page.reload()
  await app.payments.expectLoaded()
  await expect(app.page).toHaveURL(/status=CAPTURED/)
  await expect(app.page).toHaveURL(/currency=EUR/)
  await expect(app.page).toHaveURL(/minAmount=10000/)
  await expect(app.page).not.toHaveURL(/[?&]page=/)
  await expect(app.page).not.toHaveURL(/[?&]size=/)

  const subject = await sessionUserId(app.page)
  const stored = await app.page.localStorage.getItem(`pq.payment-views.${subject}`)
  ok(stored, `localStorage ${`pq.payment-views.${subject}`} must hold the saved view`)
  const parsed = savedViewsSchema.parse(JSON.parse(stored))
  const view = parsed.find(entry => entry.name === LARGE_EUR_VIEW)
  expect(view?.filters).toMatchObject({
    status: 'CAPTURED',
    currency: 'EUR',
    minAmount: 10000,
    sort: 'createdAt,desc',
  })
  expect(view?.filters).not.toHaveProperty('page')
  expect(view?.filters).not.toHaveProperty('size')
})

test('PW-OPS-E2E-141 saved view storage has no access token or JWT', async ({
  app,
  ownedMerchantId,
}) => {
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(LARGE_EUR_VIEW)

  await expectNoTokenInBrowserStorage(app.page)

  const subject = await sessionUserId(app.page)
  const [local, session] = await Promise.all([app.page.localStorage.items(), app.page.sessionStorage.items()])
  const snapshot = { local, session }
  const viewKeys = snapshot.local.filter(entry => entry.name.startsWith('pq.payment-views.'))
  expect(viewKeys.map(entry => entry.name)).toEqual([`pq.payment-views.${subject}`])
  const blob = JSON.stringify(snapshot)
  expect(blob).not.toMatch(/access_token/i)
  expect(blob).not.toMatch(/Bearer /)
  expect(blob).not.toMatch(/eyJ/)
})

test('PW-OPS-API-050 payment views CRUD through BFF cookie; other user 404', async ({
  api,
  actors,
}, testInfo) => {
  const client = api
  const name = `API view ${uniqueToken()}`
  const created = await client.identity.createPaymentView({
    name,
    filters: { status: 'CAPTURED', currency: 'EUR', minAmount: 10000, sort: 'createdAt,desc' },
    columns: ['clientOrderReference', 'amountMinor', 'status', 'createdAt'],
  })
  expectStatus(created, 201)
  expect(created.body.id).toBeTruthy()
  expect(created.body.filters).toMatchObject({ status: 'CAPTURED', currency: 'EUR', minAmount: 10000 })
  expect(created.body.filters).not.toHaveProperty('page')

  const listed = await client.identity.listPaymentViews()
  expectStatus(listed, 200)
  expect(listed.body.content?.some(view => view.id === created.body.id && view.name === name)).toBe(true)

  const renamed = `${name} updated`
  const updated = await client.identity.updatePaymentView(created.body.id!, {
    name: renamed,
    filters: { status: 'CAPTURED', currency: 'EUR', minAmount: 10000, sort: 'createdAt,desc' },
    columns: ['clientOrderReference', 'status'],
  })
  expectStatus(updated, 200)
  expect(updated.body.name).toBe(renamed)

  const operator = await actors.open('platformOperator')
  const otherGet = await operator.api.identity.updatePaymentView(created.body.id!, {
    name: renamed,
    filters: { status: 'AUTHORIZED' },
  })
  expectStatus(otherGet, 404)
  const otherDelete = await operator.api.identity.deletePaymentView(created.body.id!)
  expectStatus(otherDelete, 404)

  const deleted = await client.identity.deletePaymentView(created.body.id!)
  expectStatus(deleted, 204)
  const after = await client.identity.listPaymentViews()
  expectStatus(after, 200)
  expect(after.body.content?.some(view => view.id === created.body.id)).toBe(false)
  void testInfo
})

test('PW-OPS-E2E-142 save API then logout login restores view', async ({
  app,
  api,
  ownedMerchantId,
}, testInfo) => {
  const client = api
  const name = `Large EUR captured ${uniqueToken()}`
  await test.step('save the large-EUR filter as a view and confirm it persists', async () => {
    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await app.payments.filters.applyLargeEurCaptured()
    await app.payments.views.saveAs(name)
    const stored = await client.identity.listPaymentViews()
    expectStatus(stored, 200)
    expect(stored.body.content?.some(view => view.name === name)).toBe(true)
  })
  await test.step('sign out and sign back in as the same manager', async () => {
    await app.userMenu.signOut()
    await app.page.localStorage.clear()
    const account = merchantManagerAccountForWorker(testInfo.parallelIndex % POM_WORKER_COUNT)
    await app.page.goto('/auth/keycloak')
    const username = app.login.keycloakUsernameOrEmail()
    if (await username.isVisible().catch(() => false)) {
      await username.fill(account.username)
      await app.login.keycloakPassword().fill(account.password)
      await app.login.keycloakSubmit().click()
    }
    await app.page.waitForURL(/\/admin\//, { timeout: 30_000 })
  })
  await test.step('verify the restored view is listed after re-login', async () => {
    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await app.payments.views.openMenuButton().click()
    await expect(app.payments.views.item(name)).toBeVisible()
  })
})

test('PW-OPS-E2E-143 other user does not see saved view', async ({
  app,
  actors,
  ownedMerchantId,
}, testInfo) => {
  const name = `Hidden from operator ${uniqueToken()}`
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(name)

  const { app: operatorApp } = await actors.open('platformOperator')
  await operatorApp.payments.gotoForMerchant(ownedMerchantId)
  await operatorApp.payments.expectLoaded()
  await operatorApp.payments.views.openMenuButton().click()
  await expect(operatorApp.payments.views.item(name)).toHaveCount(0)
  void testInfo
})

test('PW-OPS-E2E-144 apply view writes payment list query string', async ({
  app,
  ownedMerchantId,
}) => {
  const name = `URL view ${uniqueToken()}`
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(name)
  await app.payments.filters.clear()
  await expect(app.page).not.toHaveURL(/status=CAPTURED/)
  await app.payments.views.open(name)
  await expect(app.page).toHaveURL(/status=CAPTURED/)
  await expect(app.page).toHaveURL(/currency=EUR/)
  await expect(app.page).toHaveURL(/minAmount=10000/)
})

test('PW-OPS-E2E-145 Back restores applied view', async ({
  app,
  ownedMerchantId,
}) => {
  const name = `Back view ${uniqueToken()}`
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(name)
  await app.payments.views.open(name)
  await expect(app.page).toHaveURL(/status=CAPTURED/)
  await app.payments.openCreate()
  await expect(app.page).toHaveURL(/\/payments\/new/)
  await app.page.goBack()
  await app.payments.expectLoaded()
  await expect(app.page).toHaveURL(/status=CAPTURED/)
  await expect(app.page).toHaveURL(/currency=EUR/)
  await app.payments.views.openMenuButton().click()
  await expect(app.payments.views.item(name)).toBeVisible()
})

test('PW-OPS-E2E-146 set default star posts default', async ({
  app,
  api,
  ownedMerchantId,
}) => {
  const client = api
  const name = `Default star ${uniqueToken()}`
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(name)
  expect(await app.payments.views.setDefault(name)).toBe(200)
  await app.page.keyboard.press('Escape')
  const listed = await client.identity.listPaymentViews()
  expectStatus(listed, 200)
  expect(listed.body.content?.find(view => view.name === name)?.isDefault).toBe(true)
  await app.payments.filters.clear()
  await app.payments.views.openMenuButton().click()
  await expect(app.payments.views.item(name)).toBeVisible()
})

test('PW-OPS-E2E-147 uncheck Created by hides header; API field remains', async ({
  app,
  api,
  ownedMerchantId,
}, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'COL')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    { amountMinor: 15000, currency: 'EUR', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'COL'),
  )
  expectStatus(created, 201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  if (await app.payments.filters.clearButton().isVisible()) {
    await app.payments.filters.clear()
  }
  await expect(app.payments.referenceInTable(reference)).toBeVisible()
  await expect(app.payments.columnHeader('Created by')).toBeVisible()
  await app.payments.filters.uncheckColumn('Created by')
  await expect(app.payments.columnHeader('Created by')).toHaveCount(0)

  const listed = await client.payments.list(ownedMerchantId)
  expectStatus(listed, 200)
  const row = listed.body.content?.find(item => item.clientOrderReference === reference)
  ok(row, 'saved payment must be listed')
  expect(row.createdAt).toBeTruthy()
})
import { ok } from 'node:assert/strict'
