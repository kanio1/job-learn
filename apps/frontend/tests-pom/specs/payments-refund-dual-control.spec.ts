import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'
import { dualControlSteps } from '../methods/combinations/DualControlStDt'

test.use({ storageState: pomAuthFiles.merchantManager })

test('maker cannot self-approve a real payment refund; platform checker can', { tag: ['@serial'] }, async ({
  api,
  app,
  browser,
}, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  expect(dualControlSteps[0].expectStatus).toBe(409)
  const reference = uniqueOrderReference(testInfo, 'DC')
  const created = await api.createPaymentOrder(
    merchantAlphaId,
    { amountMinor: 2500, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, 'CREATE'),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  const detail = await api.getPaymentOrder(merchantAlphaId, paymentOrderId)
  const authorized = await api.authorizePayment(
    merchantAlphaId,
    paymentOrderId,
    detail.headers.etag!,
    uniqueIdempotencyKey(testInfo, 'AUTH'),
  )
  expect(authorized.status).toBe(200)
  const captured = await api.capturePayment(
    merchantAlphaId,
    paymentOrderId,
    authorized.headers.etag!,
    uniqueIdempotencyKey(testInfo, 'CAP'),
    2500,
  )
  expect(captured.status).toBe(200)

  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await app.page.getByTestId('refund-approval-create').click()
  await expect(app.page.getByText('PENDING')).toBeVisible()

  await app.page.getByTestId('refund-approval-approve').click()
  await expect(app.page.getByText(/dual_control_self_approve|Maker cannot approve/i)).toBeVisible()

  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const admin = new App(await adminContext.newPage())
  try {
    await admin.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await admin.paymentDetail.expectLoaded()
    await admin.page.getByTestId('refund-approval-approve').click()
    await expect(admin.paymentDetail.statusInDetail('Refunded')).toBeVisible({ timeout: 20_000 })
  } finally {
    await adminContext.close()
  }
})
