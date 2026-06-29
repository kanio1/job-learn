/**
 * F-C3: Audit Log Export Download
 *
 * Playwright/SDET capabilities demonstrated:
 *   - page.waitForEvent('download') for a second independent download scenario
 *   - download.suggestedFilename() and download.path()
 *   - JSON.parse of downloaded compliance export content
 *   - page.waitForResponse() for Content-Disposition/Content-Type assertions
 *   - testInfo.attach() with safe artifact metadata only
 */

import { readFile } from 'node:fs/promises'
import { expect, type Page, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'
import { expectNoTokenInBrowserStorage } from '../support/browser-safety-assertions'
import { expectNoAuthorizationInNetworkResponse } from '../support/network-assertions'

const eventId = '10000000-0000-4000-8000-000000000017'

const auditListResponse = {
  content: [
    {
      id: eventId,
      occurredAt: '2026-06-19T10:00:00Z',
      actorDisplay: 'Visible Operator',
      action: 'MERCHANT_CREATED',
      targetType: 'MERCHANT',
      targetId: 'merchant-17',
      tenantId: 'TENANT_ALPHA',
      correlationId: 'audit-correlation-17',
      outcome: 'SUCCESS',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
}

const auditExportResponse = {
  content: [
    {
      eventId,
      occurredAt: '2026-06-19T10:00:00Z',
      actorDisplay: 'Visible Operator',
      action: 'MERCHANT_CREATED',
      targetType: 'MERCHANT',
      targetId: 'merchant-17',
      correlationId: 'audit-correlation-17',
      outcome: 'SUCCESS',
    },
  ],
  page: 0,
  size: 100,
  totalElements: 1,
  totalPages: 1,
}

test('audit export downloads parseable JSON with safe fields and headers', async ({ page }, testInfo) => {
  await mockAuditPage(page)

  await page.goto('/admin/audit?action=MERCHANT_CREATED&from=2026-06-01&to=2026-06-30')
  await expect(page.getByTestId('audit-table')).toBeVisible({ timeout: 15000 })

  const exportResponsePromise = page.waitForResponse(response => {
    const url = new URL(response.url())
    return response.request().method() === 'GET'
      && url.pathname === '/api/audit/export.json'
      && url.searchParams.get('action') === 'MERCHANT_CREATED'
      && url.searchParams.get('from') === '2026-06-01'
      && url.searchParams.get('to') === '2026-06-30'
  })
  const downloadPromise = page.waitForEvent('download')

  await page.getByTestId('export-audit-log').click()

  const [exportResponse, download] = await Promise.all([exportResponsePromise, downloadPromise])

  expectNoAuthorizationInNetworkResponse(exportResponse)
  expect(exportResponse.headers()['content-disposition']).toContain('attachment')
  expect(exportResponse.headers()['content-disposition']).toContain('audit-events.json')
  expect(exportResponse.headers()['content-type']).toContain('application/json')
  expect(download.suggestedFilename()).toBe('audit-events.json')

  const filePath = await download.path()
  expect(filePath).toBeTruthy()

  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const content = await readFile(filePath!, 'utf-8')
  const parsed = JSON.parse(content) as typeof auditExportResponse

  expect(parsed.content).toHaveLength(1)
  expect(parsed.content[0]).toMatchObject({
    eventId,
    actorDisplay: 'Visible Operator',
    action: 'MERCHANT_CREATED',
    targetType: 'MERCHANT',
    targetId: 'merchant-17',
    correlationId: 'audit-correlation-17',
    outcome: 'SUCCESS',
  })
  expect(parsed.content[0]).not.toHaveProperty('actorSubject')
  expect(parsed.content[0]).not.toHaveProperty('tenantId')

  expect(content).not.toContain('Bearer ')
  expect(content).not.toContain('Authorization')
  expect(content).not.toContain('eyJ')
  expect(content).not.toContain('session')
  expect(content).not.toContain('stack trace')

  await testInfo.attach('audit-export-summary', {
    body: JSON.stringify({
      filename: download.suggestedFilename(),
      exportedRecords: parsed.content.length,
      contentType: exportResponse.headers()['content-type'],
    }, null, 2),
    contentType: 'application/json',
  })

  await expectNoTokenInBrowserStorage(page)
})

async function mockAuditPage(page: Page): Promise<void> {
  await mockRoleSession(page, 'PLATFORM_ADMIN')

  await page.route('**/api/audit/export.json**', route =>
    route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Disposition': 'attachment; filename="audit-events.json"',
        'Cache-Control': 'no-store',
        'X-Correlation-ID': 'audit-export-correlation',
      },
      body: JSON.stringify(auditExportResponse),
    }),
  )

  await page.route('**/api/audit**', route => {
    const url = new URL(route.request().url())
    if (url.pathname !== '/api/audit') {
      return route.fallback()
    }

    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(auditListResponse),
    })
  })
}
