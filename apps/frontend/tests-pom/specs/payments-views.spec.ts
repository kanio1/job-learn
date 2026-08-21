import { merchantManagerAccountForWorker, POM_WORKER_COUNT } from '../auth/accounts'
import { BffClient } from '../api/bff-client'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { App } from '../pages/App'
import { pomAuthFiles, pomBrowserBaseURL, pomNodeBaseURL } from '../utils/env'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

const LARGE_EUR_VIEW = 'Large EUR captured'

async function sessionUserId(page: import('@playwright/test').Page): Promise<string> {
  const session = await page.evaluate(async () => {
    const response = await fetch('/api/_auth/session')
    if (!response.ok) {
      throw new Error(`session ${response.status}`)
    }
    return await response.json() as { user?: { id?: string } }
  })
  const id = session.user?.id
  expect(id, 'sanitized session user id (JWT sub) must be present').toBeTruthy()
  return id!
}

test('PW-OPS-E2E-140 save payment view then reload restores filters', async ({
  app,
  api,
  ownedMerchantId,
}, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'VIEW')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 15000, currency: 'EUR', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'VIEW'),
  )
  expect(created.status).toBe(201)

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
  const stored = await app.page.evaluate((key) => window.localStorage.getItem(key), `pq.payment-views.${subject}`)
  expect(stored, `localStorage ${`pq.payment-views.${subject}`} must hold the saved view`).toBeTruthy()
  const parsed = JSON.parse(stored!) as Array<{ name?: string, filters?: Record<string, unknown> }>
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
  const snapshot = await app.page.evaluate(() => ({
    local: Object.entries(window.localStorage),
    session: Object.entries(window.sessionStorage),
  }))
  const viewKeys = snapshot.local.filter(([key]) => key.startsWith('pq.payment-views.'))
  expect(viewKeys.map(([key]) => key)).toEqual([`pq.payment-views.${subject}`])
  const blob = JSON.stringify(snapshot)
  expect(blob).not.toMatch(/access_token/i)
  expect(blob).not.toMatch(/Bearer /)
  expect(blob).not.toMatch(/eyJ/)
})

test('PW-OPS-API-050 payment views CRUD through BFF cookie; other user 404', async ({
  playwright,
  api,
}, testInfo) => {
  const client = requireApi(api)
  const name = `API view ${uniqueToken()}`
  const created = await client.createPaymentView({
    name,
    filters: { status: 'CAPTURED', currency: 'EUR', minAmount: 10000, sort: 'createdAt,desc' },
    columns: ['clientOrderReference', 'amountMinor', 'status', 'createdAt'],
  })
  expect(created.status).toBe(201)
  expect(created.body.id).toBeTruthy()
  expect(created.body.filters).toMatchObject({ status: 'CAPTURED', currency: 'EUR', minAmount: 10000 })
  expect(created.body.filters).not.toHaveProperty('page')

  const listed = await client.listPaymentViews()
  expect(listed.status).toBe(200)
  expect(listed.body.content?.some(view => view.id === created.body.id && view.name === name)).toBe(true)

  const renamed = `${name} updated`
  const updated = await client.updatePaymentView(created.body.id!, {
    name: renamed,
    filters: { status: 'CAPTURED', currency: 'EUR', minAmount: 10000, sort: 'createdAt,desc' },
    columns: ['clientOrderReference', 'status'],
  })
  expect(updated.status).toBe(200)
  expect(updated.body.name).toBe(renamed)

  const operator = await BffClient.create(playwright, pomAuthFiles.platformOperator, pomNodeBaseURL())
  try {
    const otherGet = await operator.updatePaymentView(created.body.id!, {
      name: renamed,
      filters: { status: 'AUTHORIZED' },
    })
    expect(otherGet.status).toBe(404)
    const otherDelete = await operator.deletePaymentView(created.body.id!)
    expect(otherDelete.status).toBe(404)
  }
  finally {
    await operator.dispose()
  }

  const deleted = await client.deletePaymentView(created.body.id!)
  expect(deleted.status).toBe(204)
  const after = await client.listPaymentViews()
  expect(after.body.content?.some(view => view.id === created.body.id)).toBe(false)
  void testInfo
})

test('PW-OPS-E2E-142 save API then logout login restores view', async ({
  app,
  api,
  ownedMerchantId,
}, testInfo) => {
  const client = requireApi(api)
  const name = `Large EUR captured ${uniqueToken()}`
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(name)
  const stored = await client.listPaymentViews()
  expect(stored.body.content?.some(view => view.name === name)).toBe(true)

  await app.userMenu.signOut()
  await app.page.evaluate(() => window.localStorage.clear())
  const account = merchantManagerAccountForWorker(testInfo.parallelIndex % POM_WORKER_COUNT)
  await app.page.goto('/auth/keycloak')
  const username = app.page.getByLabel('Username or email')
  if (await username.isVisible().catch(() => false)) {
    await username.fill(account.username)
    await app.page.getByRole('textbox', { name: 'Password' }).fill(account.password)
    await app.page.getByRole('button', { name: /sign in/i }).click()
  }
  await app.page.waitForURL(/\/admin\//, { timeout: 30_000 })

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.views.openMenuButton().click()
  await expect(app.payments.views.item(name)).toBeVisible()
})

test('PW-OPS-E2E-143 other user does not see saved view', async ({
  app,
  browser,
  ownedMerchantId,
}, testInfo) => {
  const name = `Hidden from operator ${uniqueToken()}`
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(name)

  const operatorContext = await browser.newContext({
    storageState: pomAuthFiles.platformOperator,
    baseURL: pomBrowserBaseURL(),
  })
  const operatorPage = await operatorContext.newPage()
  const operatorApp = new App(operatorPage)
  try {
    await operatorApp.payments.gotoForMerchant(ownedMerchantId)
    await operatorApp.payments.expectLoaded()
    await operatorApp.payments.views.openMenuButton().click()
    await expect(operatorApp.payments.views.item(name)).toHaveCount(0)
  }
  finally {
    await operatorContext.close()
  }
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
  const client = requireApi(api)
  const name = `Default star ${uniqueToken()}`
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  await app.payments.filters.applyLargeEurCaptured()
  await app.payments.views.saveAs(name)
  await app.payments.views.setDefault(name)
  await app.page.keyboard.press('Escape')
  const listed = await client.listPaymentViews()
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
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'COL')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 15000, currency: 'EUR', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'COL'),
  )
  expect(created.status).toBe(201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()
  if (await app.payments.filters.clearButton().isVisible()) {
    await app.payments.filters.clear()
  }
  await app.payments.expectReferenceVisible(reference)
  await expect(app.page.getByRole('columnheader', { name: 'Created by' })).toBeVisible()
  await app.payments.filters.uncheckColumn('Created by')
  await expect(app.page.getByRole('columnheader', { name: 'Created by' })).toHaveCount(0)

  const listed = await client.listPaymentOrders(ownedMerchantId)
  expect(listed.status).toBe(200)
  const row = listed.body.content?.find(item => item.clientOrderReference === reference)
  expect(row).toBeTruthy()
  expect((row as { createdAt?: string }).createdAt).toBeTruthy()
})
