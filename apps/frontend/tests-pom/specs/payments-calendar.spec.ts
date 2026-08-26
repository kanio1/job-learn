import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { etagOf } from '../utils/http'

test.describe('Payment expiry calendar', () => {
  test('PW-M360-E2E-130 clock.install shows UCalendar day 20', async ({ app, page, ownedMerchantId }) => {
    await page.clock.install({ time: new Date('2026-08-20T12:00:00.000Z') })
    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await app.payments.openCalendar()
    await expect(app.payments.calendar().getByRole('button', { name: / 20,/ })).toBeVisible()
  })

  test('PW-M360-E2E-131 past days are disabled', async ({ app, page, ownedMerchantId }) => {
    await page.clock.install({ time: new Date('2026-08-20T12:00:00.000Z') })
    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await app.payments.openCalendar()
    await expect(app.payments.calendar().getByRole('button', { name: /19/ }).first()).toBeDisabled()
  })

  test('PW-M360-E2E-131b calendar lists dual-control due from refund-approvals API', async ({
    app,
    api,
    ownedMerchantId,
  }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'DUE')
    const amountMinor = 2400
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'DUEC'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId!
    const authorized = await client.authorizePayment(
      ownedMerchantId,
      paymentOrderId,
      etagOf(created.headers),
      uniqueIdempotencyKey(testInfo, 'DUEA'),
    )
    expectStatus(authorized, 200)
    const captured = await client.capturePayment(
      ownedMerchantId,
      paymentOrderId,
      etagOf(authorized.headers),
      uniqueIdempotencyKey(testInfo, 'DUEP'),
      amountMinor,
    )
    expectStatus(captured, 200)
    expect((await client.createRefundApproval(ownedMerchantId, paymentOrderId)).status).toBe(201)

    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await app.payments.openCalendar()
    await expect(app.payments.calendar()).toContainText(`Dual-control · ${reference}`)
  })
})

test.describe('Payment history timeline', () => {
  test('PW-M360-E2E-133 history tab timeline matches lifecycle transitions', async ({
    app,
    api,
    ownedMerchantId,
  }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'TL')
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 2300, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'TL'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId!
    expect((await client.authorizePayment(
      ownedMerchantId,
      paymentOrderId,
      etagOf(created.headers),
      uniqueIdempotencyKey(testInfo, 'TLA'),
    )).status).toBe(200)

    const history = await client.getPaymentOrderHistory(ownedMerchantId, paymentOrderId)
    expectStatus(history, 200)
    const toStatuses = (history.body.content ?? []).map(entry => entry.toStatus)

    await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await app.paymentDetail.openHistoryTab()
    await expect(app.paymentDetail.historyTimeline()).toBeVisible()
    for (const status of toStatuses) {
      if (status) {
        await expect(app.paymentDetail.historyTimeline()).toContainText(status)
      }
    }
  })
})
