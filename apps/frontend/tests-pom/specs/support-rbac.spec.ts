import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { BffClient , expectStatus } from '../api/bff-client'
import { pomAuthFiles } from '../utils/env'
import { test, expect } from '../fixtures'
import { App } from '../pages/App'

test('support agent sees the registry but not create; notes POST is 201 on the live BFF', { tag: ['@security'] }, async ({ browser, playwright }, testInfo) => {
  const managerApi = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  const supportContext = await browser.newContext({ storageState: pomAuthFiles.supportAgent })
  const page = await supportContext.newPage()
  const app = new App(page)
  try {
    const created = await managerApi.createPaymentOrder(
      merchantAlphaId,
      { amountMinor: 2100, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'SUPNOTE') },
      uniqueIdempotencyKey(testInfo, 'SUPNOTE'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId
    expect(paymentOrderId).toBeTruthy()

    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(page.getByTestId('action-create-merchant')).toHaveCount(0)
    await app.sidebar.expectAuditVisible(true)
    await app.sidebar.expectUsersVisible(false)

    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId!)
    await app.paymentDetail.expectLoaded()
    await expect(page.getByTestId('lifecycle-authorize')).toHaveCount(0)
    await expect(page.getByTestId('payment-note-body')).toBeVisible()

    const body = `support note ${uniqueToken()}`
    const restNote = await BffClient.create(playwright, pomAuthFiles.supportAgent)
    try {
      const postedRest = await restNote.postNote(merchantAlphaId, paymentOrderId!, body)
      expectStatus(postedRest, 201)
      const notes = await restNote.listNotes(merchantAlphaId, paymentOrderId!)
      expectStatus(notes, 200)
      expect(JSON.stringify(notes.body)).toContain(body)
    } finally {
      await restNote.dispose()
    }

    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId!)
    await app.paymentDetail.expectLoaded()
    await expect(page.getByTestId('lifecycle-authorize')).toHaveCount(0)
    await expect(page.getByTestId('payment-note-body')).toBeVisible()
    await app.paymentDetail.expectNoteVisible(body)
  } finally {
    await supportContext.close()
    await managerApi.dispose()
  }
})
