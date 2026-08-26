import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'

test.describe.configure({ mode: 'parallel' })

test('worker REST create is 201 on MERCHANT-Wn and 403 on Alpha', async ({ workerWorld }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, `W${workerWorld.index}`)
  const created = await workerWorld.api.createPaymentOrder(
    workerWorld.merchantId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, `W${workerWorld.index}`),
  )
  expectStatus(created, 201)
  expect(created.body.paymentOrderId).toBeTruthy()

  const listed = await workerWorld.api.listPaymentOrders(workerWorld.merchantId, {
    clientOrderReference: reference,
    page: 0,
    size: 20,
  })
  expectStatus(listed, 200)
  expect(listed.body?.totalElements).toBe(1)
  expect((listed.body?.content ?? []).map(row => row.paymentOrderId)).toEqual([created.body.paymentOrderId])

  const foreign = await workerWorld.api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 1600, currency: 'PLN', clientOrderReference: `${reference}-ALPHA` },
    uniqueIdempotencyKey(testInfo, `W${workerWorld.index}A`),
  )
  expectStatus(foreign, 403)
})

test('worker UI create lands on that worker merchant detail', async ({ workerWorld, workerApp, page }, testInfo) => {
  const reference = uniqueOrderReference(testInfo, `UI${workerWorld.index}`)
  const idempotencyKey = uniqueIdempotencyKey(testInfo, `UI${workerWorld.index}`)

  await workerApp.paymentCreate.gotoForMerchant(workerWorld.merchantId)
  await workerApp.paymentCreate.expectLoaded()
  await workerApp.paymentCreate.fillIdempotencyKey(idempotencyKey)
  await workerApp.paymentCreate.fillAmount(1400)
  await workerApp.paymentCreate.next()
  await workerApp.paymentCreate.chooseCurrency('PLN')
  await workerApp.paymentCreate.next()
  await workerApp.paymentCreate.fillReference(reference)
  await workerApp.paymentCreate.next()
  await workerApp.paymentCreate.submit()

  await workerApp.paymentDetail.expectLoaded()
  await expect(workerApp.page.getByTestId('payment-order-detail').getByText(reference)).toBeVisible()
  await expect(workerApp.page).toHaveURL(
    new RegExp(`/admin/merchants/${workerWorld.merchantId}/payments/[0-9a-f-]{36}`),
  )
  expect(page).not.toBe(workerApp.page)
})
