import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectNoAuthorizationInNetworkResponse, expectNoTokenInText } from '../utils/network'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

const evidenceFile = fileURLToPath(new URL('../data/files/sample-evidence.txt', import.meta.url))

test('uploads evidence on a live payment order', async ({ app, api }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, 'EVD')
  const created = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 800, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'EVD'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId
  expect(paymentOrderId).toBeTruthy()

  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId!)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.uploadEvidence(evidenceFile)

  await expect(app.page.getByTestId('evidence-file-name')).toHaveText('sample-evidence.txt')
  await expectNoTokenInBrowserStorage(app.page)
})

test('exports payment orders CSV from the list toolbar', async ({ app, page }) => {
  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()

  const exportResponsePromise = page.waitForResponse(response =>
    response.request().method() === 'GET'
    && response.url().includes(`/api/merchants/${merchantAlphaId}/payment-orders/export`),
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
  expectNoTokenInText(content, 'payment CSV export')
})
