import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { etagOf } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { paymentKanbanEdges } from '../methods/state/PaymentKanbanEdges'
import { App } from '../pages/App'
import type { Page } from '@playwright/test'
import { z } from 'zod'

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
    const client = api
    const reference = uniqueOrderReference(testInfo, 'KAN')
    const created = await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1500, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'KAN'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId

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
    const detail = await client.payments.get(ownedMerchantId, paymentOrderId)
    expect(detail.body?.status).toBe('AUTHORIZED')
  })

  test('PW-M360-E2E-091 dragTo CREATED onto AUTHORIZED', async ({ app, api, page, ownedMerchantId }, testInfo) => {
    const client = api
    const reference = uniqueOrderReference(testInfo, 'DRAG')
    const created = await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1600, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'DRAG'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId

    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await openBoardWithPrefetch(app, page, ownedMerchantId, paymentOrderId)
    const authorize = waitForBffResponse(page, {
      method: 'POST',
      pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}/authorize`,
    })
    await app.payments.dragCardTo(paymentOrderId, 'AUTHORIZED')
    expect((await authorize).status()).toBe(200)
    await expect(app.payments.stage('AUTHORIZED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
  })

  test('PW-M360-E2E-092 reload keeps AUTHORIZED card in column', async ({ app, api, ownedMerchantId }, testInfo) => {
    const client = api
    const reference = uniqueOrderReference(testInfo, 'REL')
    const created = await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1700, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'REL'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId
    const detail = await client.payments.get(ownedMerchantId, paymentOrderId)
    expect((await client.payments.authorize(
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
    actors,
    storageState,
    ownedMerchantId,
    page,
  }, testInfo) => {
    const client = api
    const storageStatePath = z.string().safeParse(storageState)
    if (!storageStatePath.success) {
      throw new Error('storageState path required')
    }
    const reference = uniqueOrderReference(testInfo, 'STALE')
    const created = await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1800, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'STALE'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId
    const afterCreate = await client.payments.get(ownedMerchantId, paymentOrderId)
    expect((await client.payments.authorize(
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

    const afterAuth = await client.payments.get(ownedMerchantId, paymentOrderId)
    expect((await client.payments.capture(
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

    const { app: otherApp } = await actors.openStorageState(storageStatePath.data)
    await otherApp.payments.gotoForMerchant(ownedMerchantId)
    await otherApp.payments.expectLoaded()
    await otherApp.payments.openBoard()
    await expect(otherApp.payments.stage('CAPTURED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
  })

  test('PW-M360-E2E-094 illegal drop CREATED onto CAPTURED is 4xx and card stays', async ({
    app,
    api,
    page,
    ownedMerchantId,
  }, testInfo) => {
    expect(paymentKanbanEdges.createdToCapturedIllegal.to).toBe('CAPTURED')
    const client = api
    const reference = uniqueOrderReference(testInfo, 'ILL')
    const created = await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1900, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'ILL'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId
    await app.payments.gotoForMerchant(ownedMerchantId)
    await app.payments.expectLoaded()
    await openBoardWithPrefetch(app, page, ownedMerchantId, paymentOrderId)
    const capture = waitForBffResponse(page, {
      method: 'POST',
      pathExact: `/api/merchants/${ownedMerchantId}/payment-orders/${paymentOrderId}/capture`,
    })
    await app.payments.dragCardTo(paymentOrderId, 'CAPTURED')
    const status = (await capture).status()
    expect(status).toBeGreaterThanOrEqual(400)
    await expect(app.payments.stage('CREATED').getByTestId(`payment-card-${paymentOrderId}`)).toBeVisible()
  })

  test('PW-M360-API-031 authorize through BffClient stays AUTHORIZED', async ({ api, ownedMerchantId }, testInfo) => {
    const client = api
    const reference = uniqueOrderReference(testInfo, 'API31')
    const created = await client.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1400, currency: 'PLN', clientOrderReference: reference },
      uniqueIdempotencyKey(testInfo, 'API31'),
    )
    expectStatus(created, 201)
    const paymentOrderId = created.body.paymentOrderId
    const detail = await client.payments.get(ownedMerchantId, paymentOrderId)
    const authorized = await client.payments.authorize(
      ownedMerchantId,
      paymentOrderId,
      etagOf(detail.headers)!,
      uniqueIdempotencyKey(testInfo, 'API31A'),
    )
    expectStatus(authorized, 200)
    expect((await client.payments.get(ownedMerchantId, paymentOrderId)).body?.status).toBe('AUTHORIZED')
  })
})
