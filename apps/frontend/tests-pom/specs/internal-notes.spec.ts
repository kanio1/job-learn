import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference, uniqueToken } from '../data/factories'
import { expectStatus } from '../api/bff-client'
import { test, expect } from '../fixtures'

test('platform admin sees notes form and can submit on a live order', { tag: ['@ux'] }, async ({ app, api, page, actors }, testInfo) => {
  const adminApi = api
  const managerApi = (await actors.open('merchantManager')).api
    const created = await managerApi.payments.createOrder(
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
    await expect(app.paymentDetail.internalNotes()).toBeVisible()
    await expect(app.paymentDetail.notesForm()).toBeVisible()

    const posted = page.waitForResponse(response =>
      response.url().includes('/notes') && response.request().method() === 'POST',
    )
    await app.paymentDetail.addNote(body)
    const response = await posted
    if (response.status() === 201) {
      await expect(app.paymentDetail.noteByText(body)).toBeVisible()
      const notes = await adminApi.payments.listNotes(merchantAlphaId, paymentOrderId!)
      expectStatus(notes, 200)
      expect(notes.body.some(note => note.body === body)).toBe(true)
    } else {
      expect(response.status()).toBe(403)
      await expect(app.paymentDetail.errorAlert()).toBeVisible()
    }
})

test('PW-M360-E2E-140 type in contenteditable POSTs notes 201', async ({ app, api, page, actors }, testInfo) => {
  const adminApi = api
  const managerApi = (await actors.open('merchantManager')).api
    const created = await managerApi.payments.createOrder(
      merchantAlphaId,
      { amountMinor: 2400, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'ED') },
      uniqueIdempotencyKey(testInfo, 'ED'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId!
    const body = `Editor note ${uniqueToken()}`

    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.paymentDetail.noteEditor()).toBeVisible()

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
    await expect(app.paymentDetail.noteByText(body)).toBeVisible()
    const notes = await adminApi.payments.listNotes(merchantAlphaId, paymentOrderId)
    expectStatus(notes, 200)
    expect(notes.body.some(note => note.body?.includes(body))).toBe(true)
})

test('PW-M360-E2E-141 stored XSS is escaped in GET and list', async ({ app, api, page, actors }, testInfo) => {
  const adminApi = api
  const managerApi = (await actors.open('merchantManager')).api
    const created = await managerApi.payments.createOrder(
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

    const notes = await adminApi.payments.listNotes(merchantAlphaId, paymentOrderId)
    expectStatus(notes, 200)
    expect(notes.body.some(note => note.body?.includes(token))).toBe(true)
    await expect(app.paymentDetail.noteByText(token)).toBeVisible()
    await expect(app.paymentDetail.noteScripts()).toHaveCount(0)
})
