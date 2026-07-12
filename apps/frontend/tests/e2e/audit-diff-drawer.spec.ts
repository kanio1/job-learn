/**
 * F-D7 — Audit before/after diff drawer.
 *
 * Playwright/SDET capabilities demonstrated:
 *   - drawer navigation (row click → GET detail → slideover content)
 *   - conditional content assertions (diff section only renders when
 *     beforeState/afterState are present on the event)
 *   - structured before/after field assertions (not just opaque text)
 */

import { expect, type Page, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'

const eventWithDiffId = '20000000-0000-4000-8000-000000000001'
const eventWithoutDiffId = '20000000-0000-4000-8000-000000000002'

const auditListResponse = {
  content: [
    {
      id: eventWithDiffId,
      occurredAt: '2026-06-19T10:00:00Z',
      actorDisplay: 'Platform Admin',
      action: 'MERCHANT_ACTIVATED',
      targetType: 'MERCHANT',
      targetId: 'merchant-diff-1',
      tenantId: 'TENANT_ALPHA',
      correlationId: 'audit-correlation-diff-1',
      outcome: 'SUCCESS',
    },
    {
      id: eventWithoutDiffId,
      occurredAt: '2026-06-19T10:05:00Z',
      actorDisplay: 'Platform Admin',
      action: 'MERCHANT_CREATED',
      targetType: 'MERCHANT',
      targetId: 'merchant-nodiff-1',
      tenantId: 'TENANT_ALPHA',
      correlationId: 'audit-correlation-nodiff-1',
      outcome: 'SUCCESS',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

const detailWithDiff = {
  ...auditListResponse.content[0],
  beforeState: { status: 'PENDING' },
  afterState: { status: 'ACTIVE' },
}

const detailWithoutDiff = {
  ...auditListResponse.content[1],
  beforeState: null,
  afterState: null,
}

test('opens the diff drawer and shows structured before/after fields (F-D7)', async ({ page }) => {
  await mockAuditPage(page)

  await page.goto('/admin/audit')
  await expect(page.getByTestId('audit-table')).toBeVisible({ timeout: 15000 })

  await page.getByTestId(`audit-row-${eventWithDiffId}`).click()

  await expect(page.getByTestId('audit-entry-drawer')).toBeVisible({ timeout: 10000 })
  await expect(page.getByTestId('audit-entry-diff')).toBeVisible()

  const row = page.getByTestId('audit-entry-diff-row').filter({ hasText: 'status' })
  await expect(row).toBeVisible()
  await expect(row.getByTestId('audit-entry-diff-before')).toHaveText('PENDING')
  await expect(row.getByTestId('audit-entry-diff-after')).toHaveText('ACTIVE')
})

test('events without before/after state render no diff section (F-D7)', async ({ page }) => {
  await mockAuditPage(page)

  await page.goto('/admin/audit')
  await expect(page.getByTestId('audit-table')).toBeVisible({ timeout: 15000 })

  await page.getByTestId(`audit-row-${eventWithoutDiffId}`).click()

  await expect(page.getByTestId('audit-entry-drawer')).toBeVisible({ timeout: 10000 })
  await expect(page.getByTestId('audit-entry-action')).toBeVisible()
  await expect(page.getByTestId('audit-entry-diff')).not.toBeVisible()
})

async function mockAuditPage(page: Page): Promise<void> {
  await mockRoleSession(page, 'PLATFORM_ADMIN')

  await page.route(`**/api/audit/${eventWithDiffId}`, route =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(detailWithDiff) }),
  )
  await page.route(`**/api/audit/${eventWithoutDiffId}`, route =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(detailWithoutDiff) }),
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
