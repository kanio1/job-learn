/**
 * F-B2: CSV Export/Download — payment orders export E2E test
 *
 * Tests the full Export CSV flow:
 *   1. Navigate to merchant payment orders page
 *   2. Click the Export CSV toolbar button (data-testid="export-payment-orders-csv")
 *   3. Use page.waitForEvent('download') to capture the file download
 *   4. Assert filename ends with .csv
 *   5. Assert CSV header row contains expected column names
 *   6. Assert no token-like content (Bearer, eyJ, Authorization) in the file
 *
 * The BFF /api/merchants/.../payment-orders/export endpoint is mocked via
 * page.route() to return a synthetic CSV with Content-Disposition: attachment,
 * which causes the browser to trigger a file download event.
 *
 * Playwright capabilities demonstrated:
 *   - page.waitForEvent('download')  — file download detection
 *   - download.suggestedFilename()   — filename assertion
 *   - download.path()                — read saved file from temp dir
 *   - page.route() + route.fulfill() with binary/text response
 */

import { readFile } from 'node:fs/promises'
import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

const merchantId = '11111111-1111-4111-8111-111111111111'

const CSV_FIXTURE = [
  'paymentOrderId,merchantId,clientOrderReference,status,amountMinor,currency,createdAt,updatedAt',
  `22222222-2222-4222-8222-222222222222,${merchantId},PAY-001,CREATED,5000,EUR,2026-06-29T10:00:00Z,2026-06-29T10:00:00Z`,
  `33333333-3333-4333-8333-333333333333,${merchantId},PAY-002,AUTHORIZED,12500,PLN,2026-06-29T09:00:00Z,2026-06-29T09:30:00Z`,
].join('\n')

test('CSV export triggers download with expected filename and column headers', async ({ page }) => {
  await mockAuthenticatedSession(page)

  // Mock the CSV export BFF endpoint — Content-Disposition: attachment causes the
  // browser to treat this as a file download rather than a page navigation.
  await page.route(`**/api/merchants/${merchantId}/payment-orders/export`, route =>
    route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'text/csv; charset=utf-8',
        'Content-Disposition': `attachment; filename="payment-orders-${merchantId}.csv"`,
        'Cache-Control': 'no-store',
      },
      body: CSV_FIXTURE,
    }),
  )

  // Mock summary so the page body loads without error (backend not running)
  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary`, route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ totalOrders: 2, totalAmountMinor: 17500, byCurrency: [], byStatus: [] }),
    }),
  )

  // Mock list — catch-all for remaining payment-orders requests; skip export/summary
  await page.route(`**/api/merchants/${merchantId}/payment-orders**`, async (route) => {
    const url = route.request().url()
    if (url.includes('/export') || url.includes('/summary')) {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments`)

  const exportBtn = page.getByTestId('export-payment-orders-csv')
  await expect(exportBtn).toBeVisible({ timeout: 15000 })

  // Register download listener before the click that triggers it
  const downloadPromise = page.waitForEvent('download')
  await exportBtn.click()
  const download = await downloadPromise

  // ── Filename assertion ────────────────────────────────────────────────────
  expect(download.suggestedFilename()).toMatch(/\.csv$/)

  // ── Content assertions ────────────────────────────────────────────────────
  const filePath = await download.path()
  expect(filePath).toBeTruthy()

  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const content = await readFile(filePath!, 'utf-8')

  // CSV must have column headers in the first row
  expect(content).toContain('paymentOrderId')
  expect(content).toContain('merchantId')
  expect(content).toContain('clientOrderReference')
  expect(content).toContain('status')
  expect(content).toContain('amountMinor')
  expect(content).toContain('currency')

  // CSV must be non-empty beyond the header
  const lines = content.trim().split('\n')
  expect(lines.length).toBeGreaterThan(1)

  // ── Token safety assertions ───────────────────────────────────────────────
  // The CSV file must never contain auth material of any kind.
  expect(content).not.toContain('Bearer ')
  expect(content).not.toContain('eyJ') // JWT header prefix
  expect(content).not.toContain('Authorization')
})
