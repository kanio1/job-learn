import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { expectStatus } from '../api/bff-client'
import { test, expect } from '../fixtures'

test('support agent sees the registry but not create; notes POST is 201 on the live BFF', { tag: ['@security'] }, async ({ actors }, testInfo) => {
  const manager = await actors.open('merchantManager')
  const support = await actors.open('supportAgent')
  const created = await manager.api.payments.createOrder(
    merchantAlphaId,
    { amountMinor: 2100, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'SUPNOTE') },
    uniqueIdempotencyKey(testInfo, 'SUPNOTE'),
  )
  expectStatus(created, 201)
  const paymentOrderId = created.body.paymentOrderId
  await support.app.merchants.goto()
  await support.app.merchants.expectRegistryTable()
  await expect(support.app.merchants.createButton()).toHaveCount(0)
  await expect(support.app.sidebar.audit()).toBeVisible()
  await expect(support.app.sidebar.users()).toHaveCount(0)
  await support.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await support.app.paymentDetail.expectLoaded()
  await expect(support.app.paymentDetail.lifecycleAction('authorize')).toHaveCount(0)
  await expect(support.app.paymentDetail.notesForm()).toBeVisible()
  const body = `support note ${uniqueToken()}`
  expectStatus(await support.api.payments.postNote(merchantAlphaId, paymentOrderId, body), 201)
  const notes = await support.api.payments.listNotes(merchantAlphaId, paymentOrderId)
  expectStatus(notes, 200)
  expect(JSON.stringify(notes.body)).toContain(body)
  await support.app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await support.app.paymentDetail.expectLoaded()
  await expect(support.app.paymentDetail.noteByText(body)).toBeVisible()
})
