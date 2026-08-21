import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { etagOf } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { paymentKanbanEdges } from '../methods/state/PaymentKanbanEdges'
import { pomBrowserBaseURL } from '../utils/env'
import { App } from '../pages/App'
import type { Page } from '@playwright/test'

async function openBoardWithPrefetch(
  app: App,
  page: Page,
  merchantId: string,
  paymentOrderId: string,
) {
  const prefetch = page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    try {
      return new URL(response.url()).pathname === `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`
    }
    catch {
      return false
    }
  })
  await app.payments.openBoard()
  await prefetch
  await expect(app.payments.card(paymentOrderId)).toBeVisible()
}

test.describe('Payment kanban', { tag: ['@kanban'] }, () => {
  test('PW-M360-E2E-090 Move menu CREATED to AUTHORIZED posts authorize', async ({
    app,
    api,
    page,
    ownedMerchantId,
  }, testInfo) => {
    expect(paymentKanbanEdges.createdToAuthorized.action).toBe('authorize')
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'KAN')
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 1500, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'KAN'),
    )
    expect(created.status).toBe(201)
    const paymentOrderId = created.body.paymentOrderId!

    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await openBoardWithPrefetch(app, page, ownedMerchantId, paymentOrderId)

    const authorize = waitForBffResponse(page, {
      method: 'POST',
      pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}/authorize`,
    })
    await app.payments.moveCardTo(paymentOrderId, 'AUTHORIZED')
    expect((await authorize).status()).toBe(200)
    await expect(app.payments.stage('AUTHORIZED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
    const detail = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    expect(detail.body?.status).toBe('AUTHORIZED')
  })

  test('PW-M360-E2E-091 dragTo CREATED onto AUTHORIZED', async ({ app, api, page, ownedMerchantId }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'DRAG')
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 1600, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'DRAG'),
    )
    expect(created.status).toBe(201)
    const paymentOrderId = created.body.paymentOrderId!

    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await openBoardWithPrefetch(app, page, ownedMerchantId, paymentOrderId)
    const authorize = waitForBffResponse(page, {
      method: 'POST',
      pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}/authorize`,
    })
    await app.payments.card(paymentOrderId).dragTo(app.payments.stage('AUTHORIZED'))
    expect((await authorize).status()).toBe(200)
    await expect(app.payments.stage('AUTHORIZED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
  })

  test('PW-M360-E2E-092 reload keeps AUTHORIZED card in column', async ({ app, api, ownedMerchantId }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'REL')
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 1700, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'REL'),
    )
    const paymentOrderId = created.body.paymentOrderId!
    const detail = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    expect((await client.authorizePayment(
      ownedMerchantId,
      paymentOrderId,
      etagOf(detail.headers)!,
      uniqueIdempotencyKey(testInfo, 'RELAUTH'),
    )).status).toBe(200)

    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await app.payments.openBoard()
    await expect(app.payments.stage('AUTHORIZED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
    await app.page.reload()
    await app.payments.openBoard()
    await expect(app.payments.stage('AUTHORIZED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
  })

  test('PW-M360-E2E-093 second context capture yields 412 rollback', async ({
    app,
    api,
    browser,
    storageState,
    ownedMerchantId,
    page,
  }, testInfo) => {
    const client = requireApi(api)
    if (typeof storageState !== 'string') {
      throw new Error('storageState path required')
    }
    const reference = uniqueOrderReference(testInfo, 'STALE')
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 1800, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'STALE'),
    )
    const paymentOrderId = created.body.paymentOrderId!
    const afterCreate = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    expect((await client.authorizePayment(
      ownedMerchantId,
      paymentOrderId,
      etagOf(afterCreate.headers)!,
      uniqueIdempotencyKey(testInfo, 'STALEAUTH'),
    )).status).toBe(200)

    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    const prefetch = page.waitForResponse(response =>
      response.request().method() === 'GET'
      && new URL(response.url()).pathname === `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}`,
    )
    await app.payments.openBoard()
    await prefetch

    const afterAuth = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    expect((await client.capturePayment(
      ownedMerchantId,
      paymentOrderId,
      etagOf(afterAuth.headers)!,
      uniqueIdempotencyKey(testInfo, 'STALECAP'),
      1800,
    )).status).toBe(200)

    const capture = waitForBffResponse(page, {
      method: 'POST',
      pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}/capture`,
    })
    await app.payments.moveCardTo(paymentOrderId, 'CAPTURED')
    expect((await capture).status()).toBe(412)
    await expect(app.payments.stage('AUTHORIZED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()

    const other = await browser.newContext({ storageState, baseURL: pomBrowserBaseURL() })
    try {
      const otherApp = new App(await other.newPage())
      await otherApp.payments.gotoForMerchant(ownedMerchantId)
      await otherApp.payments.expectLoaded()
      await otherApp.payments.openBoard()
      await expect(otherApp.payments.stage('CAPTURED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
    }
    finally {
      await other.close()
    }
  })

  test('PW-M360-E2E-094 illegal drop CREATED onto CAPTURED is 4xx and card stays', async ({
    app,
    api,
    page,
    ownedMerchantId,
  }, testInfo) => {
    expect(paymentKanbanEdges.createdToCapturedIllegal.to).toBe('CAPTURED')
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'ILL')
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 1900, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'ILL'),
    )
    const paymentOrderId = created.body.paymentOrderId!
    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await openBoardWithPrefetch(app, page, ownedMerchantId, paymentOrderId)
    const capture = waitForBffResponse(page, {
      method: 'POST',
      pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}/capture`,
    })
    await app.payments.card(paymentOrderId).dragTo(app.payments.stage('CAPTURED'))
    const status = (await capture).status()
    expect(status).toBeGreaterThanOrEqual(400)
    await expect(app.payments.stage('CREATED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
  })

  test('PW-M360-API-031 authorize through BffClient stays AUTHORIZED', async ({ api, ownedMerchantId }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueOrderReference(testInfo, 'API31')
    const created = await client.createPaymentOrder(
      ownedMerchantId,
      { amountMinor: 1400, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'API31'),
    )
    const paymentOrderId = created.body.paymentOrderId!
    const detail = await client.getPaymentOrder(ownedMerchantId, paymentOrderId)
    const authorized = await client.authorizePayment(
      ownedMerchantId,
      paymentOrderId,
      etagOf(detail.headers)!,
      uniqueIdempotencyKey(testInfo, 'API31A'),
    )
    expect(authorized.status).toBe(200)
    expect((await client.getPaymentOrder(ownedMerchantId, paymentOrderId)).body?.status).toBe('AUTHORIZED')
  })
})
