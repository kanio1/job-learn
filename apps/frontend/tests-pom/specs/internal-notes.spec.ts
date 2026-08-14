import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { BffClient } from '../api/bff-client'
import { pomAuthFiles } from '../utils/env'
import { test, expect, requireApi } from '../fixtures'

test('platform admin sees notes form and can submit on a live order', { tag: ['@ux'] }, async ({ app, api, page, playwright }, testInfo) => {
  const adminApi = requireApi(api)
  const managerApi = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  try {
    const created = await managerApi.createPaymentOrder(
      merchantAlphaId,
      { amountMinor: 2100, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'NOTE') },
      uniqueIdempotencyKey(testInfo, 'NOTE'),
    )
    expect(created.status).toBe(201)
    const paymentOrderId = created.body.paymentOrderId
    expect(paymentOrderId).toBeTruthy()

    const body = `POM note ${uniqueToken()}`
    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId!)
    await app.paymentDetail.expectLoaded()
    await expect(app.page.getByTestId('payment-internal-notes')).toBeVisible()
    await expect(app.page.getByTestId('payment-note-body')).toBeVisible()

    const posted = page.waitForResponse(response =>
      response.url().includes('/notes') && response.request().method() === 'POST',
    )
    await app.paymentDetail.addNote(body)
    const response = await posted
    if (response.status() === 201) {
      await app.paymentDetail.expectNoteVisible(body)
      const notes = await adminApi.listNotes(merchantAlphaId, paymentOrderId!)
      expect(notes.status).toBe(200)
      expect(notes.body.some(note => note.body === body)).toBe(true)
    } else {
      expect(response.status()).toBe(403)
      await expect(app.page.getByRole('alert').or(app.page.getByTestId('error-state'))).toBeVisible()
    }
  } finally {
    await managerApi.dispose()
  }
})
