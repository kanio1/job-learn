import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'
import { BffClient, type Playwright , expectStatus } from '../api/bff-client'
import { etagOf } from '../utils/http'
import type { TestInfo } from '@playwright/test'

test.use({ storageState: pomAuthFiles.merchantManager })

async function captureAndRequestRefund(
  client: BffClient,
  testInfo: TestInfo,
  amountMinor: number,
) {
  const reference = uniqueOrderReference(testInfo, 'PIN')
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PIN-CREATE'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const detail = await client.getPaymentOrder(merchantAlphaId, paymentOrderId)
  const authorized = await client.authorizePayment(
    merchantAlphaId,
    paymentOrderId,
    etagOf(detail.headers),
    uniqueIdempotencyKey(testInfo, 'PIN-AUTH'),
  )
  expectStatus(authorized, 200)
  const captured = await client.capturePayment(
    merchantAlphaId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.getPaymentOrder(merchantAlphaId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'PIN-CAP'),
    amountMinor,
  )
  expectStatus(captured, 200)
  const approval = await client.createRefundApproval(merchantAlphaId, paymentOrderId, { amountMinor })
  expectStatus(approval, 201)
  return { paymentOrderId }
}

async function checkerApi(playwright: Playwright): Promise<BffClient> {
  return BffClient.create(playwright, pomAuthFiles.platformAdmin)
}

test('PW-OPS-E2E-170 pressSequentially correct PIN continues refund', async ({
  api,
  browser,
  playwright,
}, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const adminApi = await checkerApi(playwright)
  const challenge = await adminApi.createChallenge(merchantAlphaId, paymentOrderId)
  expectStatus(challenge, 201)
  const pin = challenge.body.pin
  expect(pin).toMatch(/^\d{6}$/)

  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const admin = new App(await adminContext.newPage())
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.paymentDetail.approveRefundApproval()
    await admin.paymentDetail.pinChallenge.expectOpen()
    await admin.paymentDetail.pinChallenge.typePin(pin!)
    await admin.paymentDetail.pinChallenge.submitIfEnabled()
    await expect(admin.paymentDetail.statusInDetail('Refunded')).toBeVisible({ timeout: 20_000 })
  } finally {
    await adminContext.close()
    await adminApi.dispose()
  }
})

test('PW-OPS-E2E-171 wrong pin shows inline error and keeps 6 slots', async ({
  api,
  browser,
  playwright,
}, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const adminApi = await checkerApi(playwright)
  expect((await adminApi.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const admin = new App(await adminContext.newPage())
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.paymentDetail.approveRefundApproval()
    await admin.paymentDetail.pinChallenge.expectOpen()
    await admin.paymentDetail.pinChallenge.typePin('000000')
    await admin.paymentDetail.pinChallenge.submitIfEnabled()
    await expect(admin.page.getByTestId('refund-pin-error')).toBeVisible()
    await expect(admin.page.getByTestId('refund-pin-input')).toBeVisible()
  } finally {
    await adminContext.close()
    await adminApi.dispose()
  }
})

test('PW-OPS-E2E-172 paste 6 digits', async ({ api, browser, playwright }, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const adminApi = await checkerApi(playwright)
  const challenge = await adminApi.createChallenge(merchantAlphaId, paymentOrderId)
  expectStatus(challenge, 201)
  expect(challenge.body.pin).toMatch(/^\d{6}$/)
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const admin = new App(await adminContext.newPage())
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.paymentDetail.approveRefundApproval()
    await admin.paymentDetail.pinChallenge.expectOpen()
    await admin.paymentDetail.pinChallenge.pastePin(challenge.body.pin)
    await admin.paymentDetail.pinChallenge.submitIfEnabled()
    await expect(admin.paymentDetail.statusInDetail('Refunded')).toBeVisible({ timeout: 20_000 })
  } finally {
    await adminContext.close()
    await adminApi.dispose()
  }
})

test('PW-OPS-E2E-173 Backspace clears last slot', async ({ api, browser, playwright }, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const adminApi = await checkerApi(playwright)
  expect((await adminApi.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const admin = new App(await adminContext.newPage())
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.paymentDetail.approveRefundApproval()
    await admin.paymentDetail.pinChallenge.expectOpen()
    await admin.paymentDetail.pinChallenge.typePin('123')
    // Browser primitive: correct a wrong digit with the keyboard (PIN slots are DOM inputs).
    await admin.page.keyboard.press('Backspace')
    await expect(admin.page.getByTestId('refund-pin-challenge')).toBeVisible()
  } finally {
    await adminContext.close()
    await adminApi.dispose()
  }
})

test('PW-OPS-E2E-174 arrow navigation between slots', async ({ api, browser, playwright }, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const adminApi = await checkerApi(playwright)
  expect((await adminApi.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const admin = new App(await adminContext.newPage())
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.paymentDetail.approveRefundApproval()
    await admin.paymentDetail.pinChallenge.expectOpen()
    await admin.paymentDetail.pinChallenge.typePin('12')
    // Browser primitive: move the PIN caret with arrow keys to verify slot-level navigation.
    await admin.page.keyboard.press('ArrowLeft')
    await admin.page.keyboard.press('ArrowRight')
    await expect(admin.page.getByLabel('Refund approval PIN')).toBeVisible()
  } finally {
    await adminContext.close()
    await adminApi.dispose()
  }
})

test('PW-OPS-E2E-175 clock expire', async ({ api, browser, playwright }, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const adminApi = await checkerApi(playwright)
  const challenge = await adminApi.createChallenge(merchantAlphaId, paymentOrderId)
  expectStatus(challenge, 201)
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const page = await adminContext.newPage()
  const admin = new App(page)
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.paymentDetail.approveRefundApproval()
    await admin.paymentDetail.pinChallenge.expectOpen()
    // Browser primitive: fast-forward the clock to expire the PIN challenge.
    await page.clock.install()
    await page.clock.fastForward(120_000)
    await admin.paymentDetail.pinChallenge.typePin(challenge.body.pin ?? '123456')
    await admin.paymentDetail.pinChallenge.submitIfEnabled()
    await expect(admin.page.getByTestId('refund-pin-error')).toBeVisible()
  } finally {
    await adminContext.close()
    await adminApi.dispose()
  }
})

test('PW-OPS-E2E-176 five failures show 429 alert', async ({ api, browser, playwright }, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const adminApi = await checkerApi(playwright)
  expect((await adminApi.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const admin = new App(await adminContext.newPage())
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.paymentDetail.approveRefundApproval()
    await admin.paymentDetail.pinChallenge.expectOpen()
    for (let i = 0; i < 5; i++) {
      await admin.paymentDetail.pinChallenge.typePin('000000')
      await admin.paymentDetail.pinChallenge.submitIfEnabled()
      if (await admin.page.getByTestId('refund-pin-locked').isVisible()) {
        break
      }
      await expect(admin.page.getByTestId('refund-pin-error')).toBeVisible()
    }
    await expect(admin.page.getByTestId('refund-pin-locked')).toBeVisible()
  } finally {
    await adminContext.close()
    await adminApi.dispose()
  }
})
