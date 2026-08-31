import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectSuccess } from '../api/contracts/http-result'
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
  await expect(app.payments.asyncExportStatus()).toContainText('READY')
})

test('BFF export-jobs is 202 then READY CSV lists paymentOrderId', async ({ api, ownedMerchantId }, testInfo) => {
  const client = api
  const reference = uniqueOrderReference(testInfo, 'XJOB')
  const created = await client.payments.createOrder(
    ownedMerchantId,
    {
      amountMinor: 1300,
      currency: 'PLN',
      clientOrderReference: reference,
    },
    uniqueIdempotencyKey(testInfo, 'XJOB'),
  )
  expect(created.status).toBe(201)

  const job = await client.payments.createExportJob(ownedMerchantId)
  const acceptedJob = expectSuccess(job, 202)
  expect(locationOf(acceptedJob.headers) ?? '').toMatch(/\/export-jobs\/[0-9a-f-]+/i)
  const jobId = acceptedJob.body.jobId
  expect(jobId).toBeTruthy()

  await expect.poll(async () => {
    const polled = await client.payments.getExportJob(ownedMerchantId, jobId!)
    return polled.kind === 'success' ? polled.body.status : undefined
  }, { timeout: 15_000 }).toBe('READY')

  const csv = await client.payments.getExportJobContent(ownedMerchantId, jobId!)
  expect(csv.status).toBe(200)
  expect(csv.body).toMatch(/paymentOrderId|clientOrderReference/i)
  expect(csv.body).toContain(reference)
})
