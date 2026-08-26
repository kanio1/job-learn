import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { expectProblem } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { merchantImportJourney } from '../methods/use-case/MerchantImportJourney'

const importDir = join(dirname(fileURLToPath(import.meta.url)), '../fixtures/import')

function csvPayload(fileName: string, replacements: Record<string, string> = {}) {
  let text = readFileSync(join(importDir, fileName), 'utf8')
  for (const [token, value] of Object.entries(replacements)) {
    text = text.replaceAll(token, value)
  }
  return {
    name: fileName,
    mimeType: 'text/csv',
    buffer: Buffer.from(text, 'utf8'),
  }
}

test.describe('Merchant CSV import', () => {
  test('PW-M360-E2E-080 valid.csv preview does not insert merchants', async ({ app, api, page }, testInfo) => {
    expect(merchantImportJourney.previewThenListUnchanged).toBe('PW-M360-E2E-080')
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.openImport()
    const previewed = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants/import/preview' })
    await app.merchants.csvInput().setInputFiles(csvPayload('valid.csv', { __REF__: reference }))
    const preview = await previewed
    expect(preview.status()).toBe(200)
    const body = await preview.json() as { validCount?: number, rejectedCount?: number }
    expect(body.validCount).toBe(1)
    expect(body.rejectedCount).toBe(0)
    await expect(app.page.getByTestId('merchant-import-valid')).toHaveText('Valid: 1')
    const listed = await client.listMerchants({ q: reference })
    expectStatus(listed, 200)
    expect(listed.body?.content ?? []).toHaveLength(0)
  })

  test('PW-M360-E2E-081 invalid header is an error without commit', async ({ app, page }) => {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.openImport()
    const previewed = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants/import/preview' })
    await app.merchants.csvInput().setInputFiles(csvPayload('invalid-header.csv'))
    expect((await previewed).status()).toBe(400)
    await app.problem.expectVisible()
    await expect(app.page.getByTestId('merchant-import-commit')).toBeDisabled()
  })

  test('PW-M360-E2E-082 empty.csv is validation', async ({ app, page }) => {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.openImport()
    const previewed = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants/import/preview' })
    await app.merchants.csvInput().setInputFiles(csvPayload('empty.csv'))
    expect((await previewed).status()).toBe(400)
    await app.problem.expectVisible()
  })

  test('PW-M360-E2E-083 duplicate existing reference is rejected', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Dup ${reference}`)
    expectStatus(created, 201)
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.openImport()
    const previewed = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants/import/preview' })
    await app.merchants.csvInput().setInputFiles(csvPayload('duplicate.csv', { __REF__: reference }))
    const preview = await previewed
    expect(preview.status()).toBe(200)
    const body = await preview.json() as { rejectedCount?: number, validCount?: number }
    expect(body.rejectedCount).toBe(1)
    expect(body.validCount).toBe(0)
    await expect(app.page.getByTestId('merchant-import-rejected')).toHaveText('Rejected: 1')
    const downloadPromise = page.waitForEvent('download')
    await app.page.getByRole('button', { name: 'Download rejected' }).click()
    const download = await downloadPromise
    expect(download.suggestedFilename()).toBe('rejected-merchants.csv')
  })

  test('PW-M360-E2E-084 commit shows the merchant in the registry', async ({ app, page }, testInfo) => {
    const reference = uniqueMerchantReference(testInfo)
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.openImport()
    const previewed = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants/import/preview' })
    await app.merchants.csvInput().setInputFiles(csvPayload('utf8.csv', { __REF__: reference }))
    expect((await previewed).status()).toBe(200)
    const committed = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants/import/commit' })
    await app.page.getByTestId('merchant-import-commit').click()
    expect((await committed).status()).toBe(200)
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)
    await expect(app.page.getByText('Żółć Import Merchant')).toBeVisible()
  })

  test('PW-M360-E2E-085 second commit of the same preview is 409', async ({ api }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const preview = await client.previewMerchantImport(csvPayload('valid.csv', { __REF__: reference }))
    expectStatus(preview, 200)
    expect(preview.body?.previewId).toBeTruthy()
    const first = await client.commitMerchantImport(preview.body.previewId)
    expectStatus(first, 200)
    expect(first.body?.createdCount).toBe(1)
    const second = await client.commitMerchantImport(preview.body.previewId)
    expectStatus(second, 409)
    expectProblem(second.body, 409, 'import_already_committed')
  })

  test('PW-M360-API-030 BFF multipart preview is 200', async ({ api }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const preview = await client.previewMerchantImport(csvPayload('valid.csv', { __REF__: reference }))
    expectStatus(preview, 200)
    expect(preview.body?.validCount).toBe(1)
    expect(preview.body?.previewId).toBeTruthy()
  })
})
