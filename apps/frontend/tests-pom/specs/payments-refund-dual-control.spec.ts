import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { dualControlSteps } from '../methods/combinations/DualControlStDt'
import { BffClient, expectStatus } from '../api/bff-client'
import { etagOf, expectProblem, requireEtag } from '../utils/http'
import type { TestInfo } from '@playwright/test'

test.use({ storageState: pomAuthFiles.merchantManager })

async function captureOrder(client: BffClient, testInfo: TestInfo, tag: string, amountMinor = 2500) {
  const reference = uniqueOrderReference(testInfo, tag)
  const created = await client.payments.createOrder(
    merchantAlphaId,
    { amountMinor, currency: 'PLN', clientOrderReference: reference },
    uniqueIdempotencyKey(testInfo, `${tag}-CREATE`),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId
  const detail = await client.payments.get(merchantAlphaId, paymentOrderId)
  const authorized = await client.payments.authorize(
    merchantAlphaId,
    paymentOrderId,
    etagOf(detail.headers),
    uniqueIdempotencyKey(testInfo, `${tag}-AUTH`),
  )
  expectStatus(authorized, 200)
  const captured = await client.payments.capture(
    merchantAlphaId,
    paymentOrderId,
    etagOf(authorized.headers) ?? etagOf((await client.payments.get(merchantAlphaId, paymentOrderId)).headers),
    uniqueIdempotencyKey(testInfo, `${tag}-CAP`),
    amountMinor,
  )
  expectStatus(captured, 200)
  const afterCapture = await client.payments.get(merchantAlphaId, paymentOrderId)
  return { paymentOrderId, amountMinor, etag: requireEtag(afterCapture.headers) }
}

test('maker cannot self-approve a real payment refund; platform checker can', { tag: ['@serial'] }, async ({
  api,
  app,
  actors,
}, testInfo) => {
  expect(dualControlSteps[0].expectStatus).toBe(409)
  const { paymentOrderId } = await captureOrder(api, testInfo, 'DC')

  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.dualControlHint()).toBeVisible()
  const refundPosts: string[] = []
  app.page.on('request', request => {
    const url = request.url()
    if (request.method() === 'POST' && url.includes('/refund') && !url.includes('refund-approvals')) {
      refundPosts.push(url)
    }
  })
  await app.paymentDetail.refundApprovalCreate().click()
  await expect(app.paymentDetail.refundApprovalPending()).toBeVisible()
  expect(refundPosts, 'SCN-DC-01 UI must not POST /refund').toEqual([])

  await app.paymentDetail.refundApprovalApprove().click()
  await expect(app.paymentDetail.refundApprovalSelfApprovalError()).toBeVisible()

  const admin = await actors.open('platformAdmin')
  await admin.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await admin.app.paymentDetail.expectLoaded()
  await admin.app.paymentDetail.refundApprovalApprove().click()
  await expect(admin.app.paymentDetail.statusInDetail('Refunded')).toBeVisible({ timeout: 20_000 })
})

test('dual-control refund HTTP is 409 then 201 then 409 then 200 (SCN-DC-01…04)', { tag: ['@serial'] }, async ({
  api,
  actors,
}, testInfo) => {
  const maker = api
  const checker = (await actors.open('platformAdmin')).api
    const { paymentOrderId, amountMinor, etag } = await captureOrder(maker, testInfo, 'DCREST')
    const direct = await maker.payments.refund(
      merchantAlphaId,
      paymentOrderId,
      etag,
      uniqueIdempotencyKey(testInfo, 'DC-01'),
      amountMinor,
    )
    expect(direct.status).toBe(dualControlSteps[0].expectStatus)
    expectProblem(direct.body, 409, dualControlSteps[0].error)

    const requested = await maker.payments.createRefundApproval(merchantAlphaId, paymentOrderId, {
      amountMinor,
      reason: 'pom-dc',
    })
    expectStatus(requested, dualControlSteps[1].expectStatus)
    const approvalId = requested.body?.approvalId
    ok(approvalId, 'refund approval id must be present after 201')

    const self = await maker.payments.approveRefundApproval(
      merchantAlphaId,
      paymentOrderId,
      approvalId,
      etag,
      uniqueIdempotencyKey(testInfo, 'DC-03'),
    )
    expect(self.status).toBe(dualControlSteps[2].expectStatus)
    expectProblem(self.body, 409, dualControlSteps[2].error)

    const approved = await checker.payments.approveRefundApproval(
      merchantAlphaId,
      paymentOrderId,
      approvalId,
      etag,
      uniqueIdempotencyKey(testInfo, 'DC-04'),
    )
    expect(approved.status).toBe(dualControlSteps[3].expectStatus)
    expect(approved.body?.status).toBe('REFUNDED')
})

test('refund-approval amount above captured is 422 on checker approve', { tag: ['@serial'] }, async ({
  api,
  actors,
}, testInfo) => {
  const maker = api
  const checker = (await actors.open('platformAdmin')).api
    const { paymentOrderId, amountMinor, etag } = await captureOrder(maker, testInfo, 'DCOVER', 1800)
    const requested = await maker.payments.createRefundApproval(merchantAlphaId, paymentOrderId, {
      amountMinor: amountMinor + 1,
      reason: 'too-much',
    })
    expectStatus(requested, 201)
    const approved = await checker.payments.approveRefundApproval(
      merchantAlphaId,
      paymentOrderId,
      requested.body.approvalId,
      etag,
      uniqueIdempotencyKey(testInfo, 'DCOVER'),
    )
    expectStatus(approved, 422)
    expectProblem(approved.body, 422, 'refund_amount_exceeds_captured')
})
import { ok } from 'node:assert/strict'
