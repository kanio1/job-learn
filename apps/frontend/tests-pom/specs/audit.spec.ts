import { readFile } from 'node:fs/promises'
import { uniqueMerchantReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectNoAuthorizationInNetworkResponse, expectNoTokenInText } from '../utils/network'

test('audit log filters load and export downloads JSON', async ({ app, api, page }, testInfo) => {
  const created = await api.createMerchant(uniqueMerchantReference(testInfo), 'Audit seed')
  expect(created.status).toBe(201)

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
