import { readFile } from 'node:fs/promises'
import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { expectNoAuthorizationInNetworkResponse, expectNoTokenInText } from '../utils/network'

test('audit log filters load and export downloads JSON', async ({ app, api, page }, testInfo) => {
  const created = await requireApi(api).createMerchant(uniqueMerchantReference(testInfo), 'Audit seed')
  expectStatus(created, 201)

  await app.audit.goto()
  await app.audit.expectLoaded()
  await expect(app.page.getByTestId('audit-table')).toBeVisible()

  const exportResponsePromise = page.waitForResponse(response =>
    response.request().method() === 'GET' && new URL(response.url()).pathname === '/api/audit/export.json',
  )
  const downloadPromise = page.waitForEvent('download')
  await app.audit.export()
  const [exportResponse, download] = await Promise.all([exportResponsePromise, downloadPromise])

  expectNoAuthorizationInNetworkResponse(exportResponse)
  expect(download.suggestedFilename()).toMatch(/\.json$/i)

  const filePath = await download.path()
  expect(filePath).toBeTruthy()
  const content = await readFile(filePath!, 'utf-8')
  expectNoTokenInText(content, 'audit JSON export')
  const parsed = JSON.parse(content) as { content?: unknown[] }
  expect(Array.isArray(parsed.content) || Array.isArray(parsed)).toBe(true)
})

test('opening an audit row shows the entry drawer', async ({ app }) => {
  await app.audit.goto()
  await app.audit.expectLoaded()
  await app.audit.openFirstRow()
  await expect(app.page.getByTestId('audit-entry-drawer')).toBeVisible()
})

test('audit action filter and GET by id match a merchant-created event', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createMerchant(uniqueMerchantReference(testInfo), 'Audit filter seed')
  expectStatus(created, 201)

  const listed = await client.listAudit({ action: 'MERCHANT_CREATED', size: 50 })
  expectStatus(listed, 200)
  const eventId = listed.body?.content?.find(entry => entry.action === 'MERCHANT_CREATED')?.id
  expect(eventId).toBeTruthy()
  const entry = await client.getAuditEntry(eventId!)
  expectStatus(entry, 200)
  expect(entry.body?.id).toBe(eventId)

  await app.audit.goto()
  await app.audit.expectLoaded()
  await app.audit.applyActionFilter('Merchant created')
  await expect(app.page).toHaveURL(/action=MERCHANT_CREATED/)
  await expect(app.page.getByTestId('audit-table')).toBeVisible()

  await app.audit.gotoEntry(eventId!)
  await expect(app.page.getByTestId('audit-entry-drawer')).toBeVisible()
})
