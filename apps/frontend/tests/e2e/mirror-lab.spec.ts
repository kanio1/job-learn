import { expect, test } from '@playwright/test'
import { readFile } from 'node:fs/promises'
import { mockAuthenticatedSession } from './merchant-support'

test('mirror lab hub is reachable when flag is on', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await page.goto('/admin/merchants')
  await expect(page.getByTestId('nav-link-mirror-lab')).toBeVisible({ timeout: 15_000 })
  await page.getByTestId('nav-link-mirror-lab').click()
  await expect(page.getByText('Three identity worlds')).toBeVisible()
  await expect(page.getByTestId('mirror-lab-card-session')).toBeVisible()
})

test('statement download can be fulfilled in mocked suite', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await page.route('**/api/mirror-lab/statements**', async (route) => {
    await route.fulfill({
      status: 200,
      headers: {
        'content-type': 'text/csv',
        'content-disposition': 'attachment; filename="statement.csv"',
      },
      body: 'date,amount\n2026-01-01,10.00\n',
    })
  })
  await page.goto('/admin/merchants')
  await page.getByTestId('nav-link-mirror-lab').click()
  await page.getByTestId('mirror-lab-open-bank').click()
  const downloadPromise = page.waitForEvent('download')
  await page.getByTestId('statement-download-csv').click()
  const download = await downloadPromise
  expect(download.suggestedFilename()).toContain('statement')
})

test('statement pdf download keeps magic bytes', async ({ page }) => {
  await mockAuthenticatedSession(page)
  const pdf = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n', 'ascii')
  await page.route('**/api/mirror-lab/statements**', async (route) => {
    await route.fulfill({
      status: 200,
      headers: {
        'content-type': 'application/pdf',
        'content-disposition': 'attachment; filename="statement.pdf"',
      },
      body: pdf,
    })
  })
  await page.goto('/admin/merchants')
  await page.getByTestId('nav-link-mirror-lab').click()
  await page.getByTestId('mirror-lab-open-bank').click()
  const downloadPromise = page.waitForEvent('download')
  await page.getByTestId('statement-download-pdf').click()
  const download = await downloadPromise
  expect(download.suggestedFilename()).toContain('statement')
  const saved = await download.path()
  expect(saved).toBeTruthy()
  const bytes = await readFile(saved!)
  expect(bytes.subarray(0, 5).toString('ascii')).toBe('%PDF-')
})
