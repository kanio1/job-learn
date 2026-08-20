import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'
import { dualControlSteps } from '../methods/combinations/DualControlStDt'
import { BffClient } from '../api/bff-client'
import { etagOf, expectProblem } from '../utils/http'
import type { TestInfo } from '@playwright/test'

test.use({ storageState: pomAuthFiles.merchantManager })

async function captureOrder(client: BffClient, testInfo: TestInfo, tag: string, amountMinor = 2500) {
  const reference = uniqueOrderReference(testInfo, tag)
  const created = await client.createPaymentOrder(
    merchantAlphaId,
    { amountMinor, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, `${tag}-CREATE`),
  )
  expect(created.status).toBe(201)
  const paymentOrderId = created.body.paymentOrderId!
  const detail = await client.getPaymentOrder(merchantAlphaId, paymentOrderId)
  const authorized = await client.authorizePayment(
    merchantAlphaId,
    paymentOrderId,
    etagOf(detail.headers),
    uniqueIdempotencyKey(testInfo, `${tag}-AUTH`),
  )
  expect(authorized.status).toBe(200)
  const captured = await client.capturePayment(
    merchantAlphaId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.getPaymentOrder(merchantAlphaId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, `${tag}-CAP`),
    amountMinor,
  )
  expect(captured.status).toBe(200)
  const afterCapture = await client.getPaymentOrder(merchantAlphaId, paymentOrderId)
  return { paymentOrderId, amountMinor, etag: etagOf(afterCapture.headers)! }
}

test('maker cannot self-approve a real payment refund; platform checker can', { tag: ['@serial'] }, async ({
  api,
  app,
  browser,
}, testInfo) => {
  if (!api) {
    throw new Error('BffClient required')
  }
  expect(dualControlSteps[0].expectStatus).toBe(409)
  const { paymentOrderId } = await captureOrder(api, testInfo, 'DC')

  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('lifecycle-refund-dual-control-hint')).toBeVisible()
  const refundPosts: string[] = []
  app.page.on('request', request => {
    const url = request.url()
    if (request.method() === 'POST' && url.includes('/refund') && !url.includes('refund-approvals')) {
      refundPosts.push(url)
    }
  })
  await app.page.getByTestId('refund-approval-create').click()
  await expect(app.page.getByText('PENDING')).toBeVisible()
  expect(refundPosts, 'SCN-DC-01 UI must not POST /refund').toEqual([])

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

test('dual-control refund HTTP is 409 then 201 then 409 then 200 (SCN-DC-01…04)', { tag: ['@serial'] }, async ({
  api,
  playwright,
}, testInfo) => {
  const maker = api
  if (!maker) {
    throw new Error('BffClient required')
  }
  const checker = await BffClient.create(playwright, pomAuthFiles.platformAdmin)
  try {
    const { paymentOrderId, amountMinor, etag } = await captureOrder(maker, testInfo, 'DCREST')
    const direct = await maker.refundPayment(
      merchantAlphaId,
      paymentOrderId,
      etag,
      uniqueIdempotencyKey(testInfo, 'DC-01'),
      amountMinor,
    )
    expect(direct.status).toBe(dualControlSteps[0].expectStatus)
    expectProblem(direct.body, 409, dualControlSteps[0].error)

    const requested = await maker.createRefundApproval(merchantAlphaId, paymentOrderId, {
      amountMinor,
      reason: 'pom-dc',
    })
    expect(requested.status).toBe(dualControlSteps[1].expectStatus)
    const approvalId = requested.body?.approvalId
    expect(approvalId).toBeTruthy()

    const self = await maker.approveRefundApproval(
      merchantAlphaId,
      paymentOrderId,
      approvalId!,
      etag,
      uniqueIdempotencyKey(testInfo, 'DC-03'),
    )
    expect(self.status).toBe(dualControlSteps[2].expectStatus)
    expectProblem(self.body, 409, dualControlSteps[2].error)

    const approved = await checker.approveRefundApproval(
      merchantAlphaId,
      paymentOrderId,
      approvalId!,
      etag,
      uniqueIdempotencyKey(testInfo, 'DC-04'),
    )
    expect(approved.status).toBe(dualControlSteps[3].expectStatus)
    expect((approved.body as { status?: string } | undefined)?.status).toBe('REFUNDED')
  } finally {
    await checker.dispose()
  }
})

test('refund-approval amount above captured is 422 on checker approve', { tag: ['@serial'] }, async ({
  api,
  playwright,
}, testInfo) => {
  const maker = api
  if (!maker) {
    throw new Error('BffClient required')
  }
  const checker = await BffClient.create(playwright, pomAuthFiles.platformAdmin)
  try {
    const { paymentOrderId, amountMinor, etag } = await captureOrder(maker, testInfo, 'DCOVER', 1800)
    const requested = await maker.createRefundApproval(merchantAlphaId, paymentOrderId, {
      amountMinor: amountMinor + 1,
      reason: 'too-much',
    })
    expect(requested.status).toBe(201)
    const approved = await checker.approveRefundApproval(
      merchantAlphaId,
      paymentOrderId,
      requested.body?.approvalId!,
      etag,
      uniqueIdempotencyKey(testInfo, 'DCOVER'),
    )
    expect(approved.status).toBe(422)
    expectProblem(approved.body, 422, 'refund_amount_exceeds_captured')
  } finally {
    await checker.dispose()
  }
})
