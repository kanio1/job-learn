import { randomUUID } from 'node:crypto'
import type { TestInfo } from '@playwright/test'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { BffClient , expectStatus } from '../api/bff-client'
import { z } from 'zod'
import { workerMerchant } from '../auth/accounts'
import { workerManagerAuthFile } from '../utils/env'
import { expectNoAuthTokenLeak, expectProblem } from '../utils/http'

const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
)

async function createOrder(api: BffClient, merchantId: string, testInfo: TestInfo, label: string) {
  const created = await api.payments.createOrder(
    merchantId,
    { amountMinor: 1200, currency: 'EUR', clientOrderReference: uniqueOrderReference(testInfo, label) },
    uniqueIdempotencyKey(testInfo, label),
  )
  expectStatus(created, 201)
  return created.body.paymentOrderId!
}

test('PW-OPS-E2E-220 upload 3 images → next → index 2', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const paymentOrderId = await createOrder(client, ownedMerchantId, testInfo, 'GAL')
  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  for (let i = 0; i < 3; i++) {
    await app.paymentDetail.uploadEvidencePayload({
      name: `shot-${i}.png`,
      mimeType: 'image/png',
      buffer: PNG,
    })
    await expect(app.paymentDetail.evidenceFile(`shot-${i}.png`)).toBeVisible()
  }
  await app.paymentDetail.evidenceCarousel.expectOpen()
  await app.paymentDetail.evidenceCarousel.goNext()
  await app.paymentDetail.evidenceCarousel.goNext()
  await expect(app.paymentDetail.evidenceCarousel.activeIndex()).toHaveAttribute('data-active-index', '2')
})

test('PW-OPS-E2E-221 ArrowRight keyboard', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const paymentOrderId = await createOrder(client, ownedMerchantId, testInfo, 'ARW')
  await client.payments.uploadEvidence(ownedMerchantId, paymentOrderId, { name: 'a.png', mimeType: 'image/png', buffer: PNG })
  await client.payments.uploadEvidence(ownedMerchantId, paymentOrderId, { name: 'b.png', mimeType: 'image/png', buffer: PNG })
  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.evidenceCarousel.expectOpen()
  await app.paymentDetail.evidenceCarousel.region().focus()
  await app.page.keyboard.press('ArrowRight')
  await expect(app.paymentDetail.evidenceCarousel.activeIndex()).toHaveAttribute('data-active-index', '1')
})

test('PW-OPS-E2E-222 download still works', async ({ app, api, page, ownedMerchantId }, testInfo) => {
  const client = api
  const paymentOrderId = await createOrder(client, ownedMerchantId, testInfo, 'DL')
  const uploaded = await client.payments.uploadEvidence(
    ownedMerchantId,
    paymentOrderId,
    { name: 'keep.png', mimeType: 'image/png', buffer: PNG },
  )
  expectStatus(uploaded, 201)
  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  const download = app.paymentDetail.evidenceDownload()
  await expect(download).toBeVisible()
  const href = await download.getAttribute('href')
  expect(href).toMatch(/\/evidence\/[0-9a-f-]+$/i)
  const downloaded = await page.request.get(href!)
  expect(downloaded.status()).toBe(200)
  expect(downloaded.headers()['content-type']).toContain('image/png')
})

test('PW-OPS-E2E-223 invalid id error slide', async ({ app, api, ownedMerchantId }, testInfo) => {
  const client = api
  const paymentOrderId = await createOrder(client, ownedMerchantId, testInfo, 'MISS')
  await client.payments.uploadEvidence(ownedMerchantId, paymentOrderId, { name: 'ok.png', mimeType: 'image/png', buffer: PNG })
  const missing = randomUUID()
  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId, `?evidence=${missing}`)
  await app.paymentDetail.expectLoaded()
  await app.paymentDetail.evidenceCarousel.expectOpen()
  await expect(app.paymentDetail.evidenceCarousel.errorSlide()).toBeVisible()
  await app.paymentDetail.evidenceCarousel.goNext()
  await expect(app.paymentDetail.evidenceCarousel.activeIndex()).toHaveAttribute('data-active-index', '1')
})

test('PW-OPS-E2E-224 other merchant evidence 404', async ({ app, api, actors, ownedMerchantId }, testInfo) => {
  const client = api
  const paymentOrderId = await createOrder(client, ownedMerchantId, testInfo, 'BOLA')
  const other = workerMerchant((testInfo.parallelIndex + 1) % 4)
  const otherApi = (await actors.openStorageState(workerManagerAuthFile(other.index))).api
  const otherOrder = await createOrder(otherApi, other.merchantId, testInfo, 'BOLA2')
  const foreign = await otherApi.payments.uploadEvidence(
    other.merchantId,
    otherOrder,
    { name: 'foreign.png', mimeType: 'image/png', buffer: PNG },
  )
  expectStatus(foreign, 201)
  const denied = await client.payments.getEvidence(ownedMerchantId, paymentOrderId, foreign.body.evidenceId)
  expect(denied.status).toBe(404)
  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId, `?evidence=${foreign.body.evidenceId}`)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.evidenceCarousel.errorSlide()).toBeVisible()
})

test('PW-OPS-API-070 GET evidence unknown id 404 problem', async ({ api, ownedMerchantId }, testInfo) => {
  const client = api
  const paymentOrderId = await createOrder(client, ownedMerchantId, testInfo, 'API070')
  const missing = randomUUID()
  const response = await client.payments.getEvidence(ownedMerchantId, paymentOrderId, missing)
  expect(response.status).toBe(404)
  const contentType = String(response.headers['content-type'] ?? '')
  expect(contentType).toContain('application/problem+json')
  const raw = response.body.toString('utf8')
  const body = z.object({ type: z.string().optional() }).passthrough().parse(JSON.parse(raw))
  expect(body).not.toHaveProperty('statusCode')
  expect(String(body.type ?? '')).toMatch(/^https:\/\/api\.payment-quality\.local\/problems\//)
  expectProblem(body, 404, 'not_found')
  expectNoAuthTokenLeak(response.headers, raw)
})
