import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { BffClient , expectStatus } from '../api/bff-client'
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
    expectStatus(created, 201)
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
      expectStatus(notes, 200)
      expect(notes.body.some(note => note.body === body)).toBe(true)
    } else {
      expect(response.status()).toBe(403)
      await expect(app.page.getByRole('alert').or(app.page.getByTestId('error-state'))).toBeVisible()
    }
  } finally {
    await managerApi.dispose()
  }
})

test('PW-M360-E2E-140 type in contenteditable POSTs notes 201', async ({ app, api, page, playwright }, testInfo) => {
  const adminApi = requireApi(api)
  const managerApi = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  try {
    const created = await managerApi.createPaymentOrder(
      merchantAlphaId,
      { amountMinor: 2400, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'ED') },
      uniqueIdempotencyKey(testInfo, 'ED'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId!
    const body = `Editor note ${uniqueToken()}`

    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.page.locator('[data-testid="payment-note-body"] [contenteditable="true"]')).toBeVisible()

    const posted = page.waitForResponse((response) => {
      try {
        return response.request().method() === 'POST'
          && new URL(response.url()).pathname
            === `/api/merchants/${merchantAlphaId}/payment-orders/${paymentOrderId}/notes`
      }
      catch {
        return false
      }
    })
    await app.paymentDetail.addNote(body)
    expect((await posted).status()).toBe(201)
    await app.paymentDetail.expectNoteVisible(body)
    const notes = await adminApi.listNotes(merchantAlphaId, paymentOrderId)
    expectStatus(notes, 200)
    expect(notes.body.some(note => note.body?.includes(body))).toBe(true)
  }
  finally {
    await managerApi.dispose()
  }
})

test('PW-M360-E2E-141 stored XSS is escaped in GET and list', async ({ app, api, page, playwright }, testInfo) => {
  const adminApi = requireApi(api)
  const managerApi = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  try {
    const created = await managerApi.createPaymentOrder(
      merchantAlphaId,
      { amountMinor: 2500, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'XSS') },
      uniqueIdempotencyKey(testInfo, 'XSS'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId!
    const token = uniqueToken()
    const xss = `<script>alert(${token})</script>`

    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    const posted = page.waitForResponse((response) => {
      try {
        return response.request().method() === 'POST'
          && new URL(response.url()).pathname
            === `/api/merchants/${merchantAlphaId}/payment-orders/${paymentOrderId}/notes`
      }
      catch {
        return false
      }
    })
    await app.paymentDetail.addNote(xss)
    expect((await posted).status()).toBe(201)

    const notes = await adminApi.listNotes(merchantAlphaId, paymentOrderId)
    expectStatus(notes, 200)
    expect(notes.body.some(note => note.body?.includes(token))).toBe(true)
    await expect(app.page.getByTestId('payment-note-item').filter({ hasText: token })).toBeVisible()
    await expect(app.page.getByTestId('payment-internal-notes').locator('script')).toHaveCount(0)
  }
  finally {
    await managerApi.dispose()
  }
})
