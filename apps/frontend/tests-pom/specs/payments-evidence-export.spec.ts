import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { expectNoAuthorizationInNetworkResponse, expectNoTokenInText } from '../utils/network'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

const evidenceFile = fileURLToPath(new URL('../data/files/sample-evidence.txt', import.meta.url))

test('uploads evidence on a live payment order', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'EVD')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 800, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'EVD'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()

  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.uploadEvidence(evidenceFile)

  await expect(app.page.getByTestId('evidence-file-name')).toHaveText('sample-evidence.txt')
  const download = app.page.getByTestId('evidence-download')
  await expect(download).toBeVisible()
  const href = await download.getAttribute('href')
  expect(href).toMatch(/\/evidence\/[0-9a-f-]+$/i)
  const downloaded = await page.request.get(href!)
  expect(downloaded.status()).toBe(200)
  expectNoAuthorizationInNetworkResponse(downloaded)
  const text = await downloaded.text()
  expect(text).toContain('Accepted evidence for live POM upload.')
  expectNoTokenInText(text, 'evidence download')
  await expectNoTokenInBrowserStorage(app.page)
})

test('BFF multipart evidence upload is 201 RECEIPT', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    {
      amountMinor: 850,
      currency: 'PLN',
      clientOrderReference: uniqueOrderReference(testInfo, 'EVREST'),
    },
    uniqueIdempotencyKey(testInfo, 'EVREST'),
  )
  expectStatus(created, 201)
  const uploaded = await client.uploadEvidence(
    ownedMerchantId,
    created.body.paymentOrderId!,
    { name: 'live-proof.txt', mimeType: 'text/plain', buffer: Buffer.from('playwright-rest-evidence') },
    'RECEIPT',
  )
  expectStatus(uploaded, 201)
  expect(uploaded.body?.evidenceId).toBeTruthy()
  expect(uploaded.body?.category).toBe('RECEIPT')

  const downloaded = await client.getEvidence(
    ownedMerchantId,
    created.body.paymentOrderId,
    uploaded.body.evidenceId,
  )
  expect(downloaded.status).toBe(200)
  expect(downloaded.headers['authorization']).toBeUndefined()
  expect(downloaded.raw.toString('utf8')).toContain('playwright-rest-evidence')
  expect(downloaded.raw.toString('utf8').includes('Bearer eyJ')).toBe(false)
})

test('exports payment orders CSV from the list toolbar', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'CSV')
  expect((await client.createPaymentOrder(
    ownedMerchantId,
    { amountMinor: 900, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CSV'),
  )).status).toBe(201)

  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()

  const exportResponsePromise = page.waitForResponse(response =>
    response.request().method() === 'GET'
    && response.url().includes(`/api/merchants/${ownedMerchantId}/payment-orders/export`),
  )
  const downloadPromise = page.waitForEvent('download')
  await app.payments.exportCsv()
  const [exportResponse, download] = await Promise.all([exportResponsePromise, downloadPromise])

  expectNoAuthorizationInNetworkResponse(exportResponse)
  expect(download.suggestedFilename()).toMatch(/\.csv$/i)

  const filePath = await download.path()
  expect(filePath).toBeTruthy()
  const content = await readFile(filePath!, 'utf-8')
  expect(content).toMatch(/paymentOrderId|clientOrderReference/i)
  expect(content).toContain(reference)
  expectNoTokenInText(content, 'payment CSV export')
})
