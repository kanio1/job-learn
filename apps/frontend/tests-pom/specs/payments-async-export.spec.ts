import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { locationOf } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'

test('async export polls until READY then downloads CSV', async ({ app, page, ownedMerchantId }) => {
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()

  const created = waitForBffResponse(page, {
    method: 'POST',
    pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/export-jobs`,
  })
  await app.payments.exportAsync()
  const job = await created
  expect(job.status()).toBe(202)
  expect(job.headers()['location'] ?? '').toMatch(/\/export-jobs\/[0-9a-f-]+/i)
  await expect(app.page.getByTestId('async-export-status')).toContainText('READY')
})

test('BFF export-jobs is 202 then READY CSV lists paymentOrderId', async ({ api, ownedMerchantId }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueOrderReference(testInfo, 'XJOB')
  const created = await client.createPaymentOrder(
    ownedMerchantId,
    {
      amountMinor: 1300,
      currency: 'PLN',
      clientOrderReference: reference,
    },
    uniqueIdempotencyKey(testInfo, 'XJOB'),
  )
  expect(created.status).toBe(201)

  const job = await client.createExportJob(ownedMerchantId)
  expect(job.status).toBe(202)
  expect(locationOf(job.headers) ?? '').toMatch(/\/export-jobs\/[0-9a-f-]+/i)
  const jobId = job.body?.jobId
  expect(jobId).toBeTruthy()

  await expect.poll(async () => {
    const polled = await client.getExportJob(ownedMerchantId, jobId!)
    return polled.body?.status
  }, { timeout: 15_000 }).toBe('READY')

  const csv = await client.getExportJobContent(ownedMerchantId, jobId!)
  expect(csv.status).toBe(200)
  expect(csv.raw).toMatch(/paymentOrderId|clientOrderReference/i)
  expect(csv.raw).toContain(reference)
})
