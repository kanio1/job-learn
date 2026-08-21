import { randomUUID } from 'node:crypto'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { merchantAlphaId } from '../auth/accounts'
import { BffClient } from '../api/bff-client'
import { pomAuthFiles } from '../utils/env'
import { etagOf } from '../utils/http'
import { test, expect, requireApi } from '../fixtures'

test.describe('Live operations feed', { tag: ['@ops-feed'] }, () => {
  test('PW-OPS-API-020 inject 201 admin / 403 readonly; token not in WS URL', async ({
    api,
    playwright,
    page,
    app,
  }) => {
    const client = requireApi(api)
    const eventId = randomUUID()
    const injected = await client.injectOpsFeed({
      eventId,
      type: 'PAYMENT_CAPTURED',
      label: 'PO-API-020  CAPTURED',
      occurredAt: '2026-08-20T10:42:03Z',
      merchantId: merchantAlphaId,
      paymentOrderId: randomUUID(),
    })
    expect(injected.status).toBe(201)
    expect(injected.body.eventId).toBe(eventId)

    const readonlyApi = await BffClient.create(playwright, pomAuthFiles.readOnlyUser)
    try {
      const denied = await readonlyApi.injectOpsFeed({
        type: 'PAYMENT_CAPTURED',
        label: 'PO-API-020-RO  CAPTURED',
      })
      expect(denied.status).toBe(403)
    }
    finally {
      await readonlyApi.dispose()
    }

    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await app.overview.expectLoaded()
    const ws = await wsPromise
    expect(ws.url()).toMatch(/\/api\/ops\/feed$/)
    expect(ws.url()).not.toMatch(/access_token|token=|Bearer/)
    const html = await page.content()
    expect(html).not.toMatch(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\./)
  })

  test('PW-OPS-E2E-120 BffClient capture → framereceived → row', async ({
    app,
    playwright,
  }, testInfo) => {
    const managerApi = await BffClient.create(playwright, pomAuthFiles.merchantManager)
    try {
      const reference = uniqueOrderReference(testInfo, 'OPS120')
      const created = await managerApi.createPaymentOrder(
        merchantAlphaId,
        { amountMinor: 1500, currency: 'PLN', clientOrderReference: reference },
        uniqueIdempotencyKey(testInfo, 'OPS120-C'),
      )
      expect(created.status).toBe(201)
      const paymentOrderId = created.body.paymentOrderId!
      const authorized = await managerApi.authorizePayment(
        merchantAlphaId,
        paymentOrderId,
        etagOf((await managerApi.getPaymentOrder(merchantAlphaId, paymentOrderId)).headers),
        uniqueIdempotencyKey(testInfo, 'OPS120-A'),
      )
      expect(authorized.status).toBe(200)

      const wsPromise = app.overview.opsFeed.attachWebSocket()
      await app.overview.goto()
      await app.overview.opsFeed.expectLoaded()
      const ws = await wsPromise
      const framePromise = ws.waitForEvent('framereceived')
      const captured = await managerApi.capturePayment(
        merchantAlphaId,
        paymentOrderId,
        etagOf((await managerApi.getPaymentOrder(merchantAlphaId, paymentOrderId)).headers),
        uniqueIdempotencyKey(testInfo, 'OPS120-P'),
        1500,
      )
      expect(captured.status).toBe(200)
      const frame = await framePromise
      expect(String(frame.payload)).toContain('PAYMENT_CAPTURED')
      await app.overview.opsFeed.waitForOpsEvent({ orderRef: reference, type: 'CAPTURED' })
    }
    finally {
      await managerApi.dispose()
    }
  })

  test('PW-OPS-E2E-121 duplicate eventId is one row', async ({ app, api }) => {
    const client = requireApi(api)
    const eventId = randomUUID()
    const payload = {
      eventId,
      type: 'PAYMENT_CAPTURED',
      label: 'PO-121  CAPTURED',
      occurredAt: '2026-08-20T10:42:03Z',
      merchantId: merchantAlphaId,
      paymentOrderId: randomUUID(),
    }
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await app.overview.opsFeed.expectConnected()
    await wsPromise
    expect((await client.injectOpsFeed(payload)).status).toBe(201)
    expect((await client.injectOpsFeed(payload)).status).toBe(201)
    await app.overview.opsFeed.waitForOpsEvent({ eventId })
    await expect(app.overview.opsFeed.rowByEventId(eventId)).toHaveCount(1)
  })

  test('PW-OPS-E2E-122 out-of-order frames sort by occurredAt', async ({ app, api }) => {
    const client = requireApi(api)
    const laterId = randomUUID()
    const earlierId = randomUUID()
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await app.overview.opsFeed.expectConnected()
    await wsPromise
    expect((await client.injectOpsFeed({
      eventId: laterId,
      type: 'PAYMENT_CAPTURED',
      label: 'PO-122-T2  CAPTURED',
      occurredAt: '2026-08-20T10:42:03Z',
    })).status).toBe(201)
    expect((await client.injectOpsFeed({
      eventId: earlierId,
      type: 'PAYMENT_AUTHORIZED',
      label: 'PO-122-T1  AUTHORIZED',
      occurredAt: '2026-08-20T10:41:01Z',
    })).status).toBe(201)
    await app.overview.opsFeed.waitForOpsEvent({ eventId: laterId })
    await app.overview.opsFeed.waitForOpsEvent({ eventId: earlierId })
    const labels = await app.overview.opsFeed.rows().locator('[data-testid="ops-feed-label"]').allTextContents()
    const earlierIndex = labels.findIndex(text => text.includes('PO-122-T1'))
    const laterIndex = labels.findIndex(text => text.includes('PO-122-T2'))
    expect(earlierIndex).toBeGreaterThanOrEqual(0)
    expect(laterIndex).toBeGreaterThan(earlierIndex)
  })

  test('PW-OPS-E2E-123 malformed toast still connected', async ({ app, api, page }) => {
    const client = requireApi(api)
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await app.overview.opsFeed.expectConnected()
    await wsPromise
    expect((await client.injectOpsFeed({ raw: '{not-json' })).status).toBe(201)
    await expect(page.locator('[data-slot="title"]').filter({ hasText: 'Ignored invalid event' })).toBeVisible()
    await app.overview.opsFeed.expectConnected()
  })

  test('PW-OPS-E2E-124 setOffline flips connected chip', async ({ app, page }) => {
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await app.overview.opsFeed.expectConnected()
    await wsPromise
    await page.context().setOffline(true)
    await app.overview.opsFeed.expectDisconnected()
    await page.context().setOffline(false)
    await app.overview.opsFeed.expectConnected()
  })

  test('PW-OPS-E2E-120…125 401 handshake → disconnected chip, no reconnect', async ({
    app,
    page,
  }) => {
    const firstWs = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await app.overview.opsFeed.expectConnected()
    await firstWs

    const deniedWs = app.overview.opsFeed.attachWebSocket()
    await page.context().clearCookies()
    await page.context().setOffline(true)
    await app.overview.opsFeed.expectDisconnected()
    await page.context().setOffline(false)
    const socket = await deniedWs
    await socket.waitForEvent('close')
    await app.overview.opsFeed.expectDisconnected()

    let extraHandshakes = 0
    page.on('websocket', (ws) => {
      if (ws.url().includes('/api/ops/feed')) {
        extraHandshakes += 1
      }
    })
    await expect.poll(async () => {
      expect((await app.overview.opsFeed.chip().textContent())?.trim()).toBe('disconnected')
      return extraHandshakes
    }, { timeout: 3500 }).toBe(0)
  })

  test('PW-OPS-E2E-125 reconnect does not duplicate eventId from recent', async ({ app, api, page }) => {
    const client = requireApi(api)
    const eventId = randomUUID()
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await app.overview.opsFeed.expectConnected()
    await wsPromise
    expect((await client.injectOpsFeed({
      eventId,
      type: 'PAYMENT_CAPTURED',
      label: 'PO-125  CAPTURED',
      occurredAt: '2026-08-20T10:42:03Z',
    })).status).toBe(201)
    await app.overview.opsFeed.waitForOpsEvent({ eventId })
    await page.context().setOffline(true)
    await app.overview.opsFeed.expectDisconnected()
    await page.context().setOffline(false)
    await app.overview.opsFeed.expectConnected()
    await expect(app.overview.opsFeed.rowByEventId(eventId)).toHaveCount(1)
  })
})
