import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'

test('platform admin adds an internal note that persists', { tag: ['@ux'] }, async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const created = await client.createPaymentOrder(
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

  await app.paymentDetail.addNote(body)
  await app.paymentDetail.expectNoteVisible(body)

  const notes = await client.listNotes(merchantAlphaId, paymentOrderId!)
  expect(notes.status).toBe(200)
  expect(notes.body.some(note => note.body === body)).toBe(true)
})
