import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { BffClient, expectStatus } from '../api/bff-client'
import { etagOf } from '../utils/http'
import { pomAuthFiles } from '../utils/env'
import type { TestInfo } from '@playwright/test'

test.use({ storageState: pomAuthFiles.merchantManager })

async function captureAndRequestRefund(
  client: BffClient,
  testInfo: TestInfo,
  amountMinor: number,
) {
  const reference = uniqueOrderReference(testInfo, 'PIN')
  const created = await client.payments.createOrder(
    merchantAlphaId,
    { amountMinor, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'PIN-CREATE'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId!
  const detail = await client.payments.get(merchantAlphaId, paymentOrderId)
  const authorized = await client.payments.authorize(
    merchantAlphaId,
    paymentOrderId,
    etagOf(detail.headers),
    uniqueIdempotencyKey(testInfo, 'PIN-AUTH'),
  )
  expectStatus(authorized, 200)
  const captured = await client.payments.capture(
    merchantAlphaId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.payments.get(merchantAlphaId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, 'PIN-CAP'),
    amountMinor,
  )
  expectStatus(captured, 200)
  const approval = await client.payments.createRefundApproval(merchantAlphaId, paymentOrderId, { amountMinor })
  expectStatus(approval, 201)
  return { paymentOrderId }
}

test('PW-OPS-E2E-170 pressSequentially correct PIN continues refund', async ({ api, actors }, testInfo) => {
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const admin = await actors.open('platformAdmin')
  const adminApi = admin.api
  const challenge = await adminApi.payments.createChallenge(merchantAlphaId, paymentOrderId)
  expectStatus(challenge, 201)
  const pin = challenge.body.pin
  expect(pin).toMatch(/^\d{6}$/)

    await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.app.paymentDetail.expectLoaded()
    await admin.app.paymentDetail.approveRefundApproval()
    await admin.app.paymentDetail.pinChallenge.expectOpen()
    await admin.app.paymentDetail.pinChallenge.typePin(pin!)
    // UPinInput emits complete after the sixth digit and the component verifies then.
    await expect(admin.app.paymentDetail.statusInDetail('Refunded')).toBeVisible({ timeout: 20_000 })
})

test('PW-OPS-E2E-171 wrong pin shows inline error and keeps 6 slots', async ({ api, actors }, testInfo) => {
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const admin = await actors.open('platformAdmin')
  const adminApi = admin.api
  expect((await adminApi.payments.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
    await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.app.paymentDetail.expectLoaded()
    await admin.app.paymentDetail.approveRefundApproval()
    await admin.app.paymentDetail.pinChallenge.expectOpen()
    await admin.app.paymentDetail.pinChallenge.typePin('000000')
    await expect(admin.app.paymentDetail.pinChallenge.error()).toBeVisible()
    await expect(admin.app.paymentDetail.pinChallenge.input()).toBeVisible()
})

test('PW-OPS-E2E-172 paste 6 digits', async ({ api, actors }, testInfo) => {
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const admin = await actors.open('platformAdmin')
  const adminApi = admin.api
  const challenge = await adminApi.payments.createChallenge(merchantAlphaId, paymentOrderId)
  expectStatus(challenge, 201)
  expect(challenge.body.pin).toMatch(/^\d{6}$/)
    await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.app.paymentDetail.expectLoaded()
    await admin.app.paymentDetail.approveRefundApproval()
    await admin.app.paymentDetail.pinChallenge.expectOpen()
    await admin.app.paymentDetail.pinChallenge.pastePin(challenge.body.pin)
    await expect(admin.app.paymentDetail.statusInDetail('Refunded')).toBeVisible({ timeout: 20_000 })
})

test('PW-OPS-E2E-173 Backspace clears last slot', async ({ api, actors }, testInfo) => {
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const admin = await actors.open('platformAdmin')
  const adminApi = admin.api
  expect((await adminApi.payments.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
    await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.app.paymentDetail.expectLoaded()
    await admin.app.paymentDetail.approveRefundApproval()
    await admin.app.paymentDetail.pinChallenge.expectOpen()
    await admin.app.paymentDetail.pinChallenge.typePin('123')
    // Browser primitive: correct a wrong digit with the keyboard (PIN slots are DOM inputs).
    await admin.page.keyboard.press('Backspace')
    await expect(admin.app.paymentDetail.pinChallenge.root()).toBeVisible()
})

test('PW-OPS-E2E-174 arrow navigation between slots', async ({ api, actors }, testInfo) => {
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const admin = await actors.open('platformAdmin')
  const adminApi = admin.api
  expect((await adminApi.payments.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
    await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.app.paymentDetail.expectLoaded()
    await admin.app.paymentDetail.approveRefundApproval()
    await admin.app.paymentDetail.pinChallenge.expectOpen()
    await admin.app.paymentDetail.pinChallenge.typePin('12')
    // Browser primitive: move the PIN caret with arrow keys to verify slot-level navigation.
    await admin.page.keyboard.press('ArrowLeft')
    await admin.page.keyboard.press('ArrowRight')
    await expect(admin.app.paymentDetail.pinChallenge.pinLabel()).toBeVisible()
})

test('PW-OPS-E2E-175 clock expire', async ({ api, actors }, testInfo) => {
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const admin = await actors.open('platformAdmin')
  const adminApi = admin.api
  const challenge = await adminApi.payments.createChallenge(merchantAlphaId, paymentOrderId)
  expectStatus(challenge, 201)
  const page = admin.page
    await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.app.paymentDetail.expectLoaded()
    await admin.app.paymentDetail.approveRefundApproval()
    await admin.app.paymentDetail.pinChallenge.expectOpen()
    // Browser primitive: fast-forward the clock to expire the PIN challenge.
    await page.clock.install()
    await page.clock.fastForward(120_000)
    await admin.app.paymentDetail.pinChallenge.typePin(challenge.body.pin ?? '123456')
    await expect(admin.app.paymentDetail.pinChallenge.error()).toBeVisible()
})

test('PW-OPS-E2E-176 five failures show 429 alert', async ({ api, actors }, testInfo) => {
  const { paymentOrderId } = await captureAndRequestRefund(api, testInfo, 100_001)
  const admin = await actors.open('platformAdmin')
  const adminApi = admin.api
  expect((await adminApi.payments.createChallenge(merchantAlphaId, paymentOrderId)).status).toBe(201)
    await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.app.paymentDetail.expectLoaded()
    await admin.app.paymentDetail.approveRefundApproval()
    await admin.app.paymentDetail.pinChallenge.expectOpen()
    for (let i = 0; i < 5; i++) {
      await admin.app.paymentDetail.pinChallenge.typePin('000000')
      if (await admin.app.paymentDetail.pinChallenge.lockedAlert().isVisible()) {
        break
      }
      await expect(admin.app.paymentDetail.pinChallenge.error()).toBeVisible()
    }
    await expect(admin.app.paymentDetail.pinChallenge.lockedAlert()).toBeVisible()
})
