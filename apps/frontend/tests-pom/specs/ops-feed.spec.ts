import { randomUUID } from 'node:crypto'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { merchantAlphaId } from '../auth/accounts'
import { expectStatus } from '../api/bff-client'
import { etagOf } from '../utils/http'
import { test, expect } from '../fixtures'

test.describe('Live operations feed', { tag: ['@ops-feed'] }, () => {
  test('PW-OPS-API-020 inject 201 admin / 403 readonly; token not in WS URL', async ({
    api,
    actors,
    page,
    app,
  }) => {
    const client = api
    const eventId = randomUUID()
    const injected = await client.operations.injectFeed({
      eventId,
      type: 'PAYMENT_CAPTURED',
      label: 'PO-API-020  CAPTURED',
      occurredAt: '2026-08-20T10:42:03Z',
      merchantId: merchantAlphaId,
      paymentOrderId: randomUUID(),
    })
    expectStatus(injected, 201)
    expect(injected.body.eventId).toBe(eventId)

    const readonlyApi = (await actors.open('readOnlyUser')).api
    const denied = await readonlyApi.operations.injectFeed({
      type: 'PAYMENT_CAPTURED',
      label: 'PO-API-020-RO  CAPTURED',
    })
    expectStatus(denied, 403)

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
    actors,
  }, testInfo) => {
    const managerApi = (await actors.open('merchantManager')).api
      const reference = uniqueOrderReference(testInfo, 'OPS120')
      let paymentOrderId: string
      await test.step('authorize a merchant payment', async () => {
        const created = await managerApi.payments.createOrder(
          merchantAlphaId,
          { amountMinor: 1500, currency: 'PLN', clientOrderReference: reference },
          uniqueIdempotencyKey(testInfo, 'OPS120-C'),
        )
        expectStatus(created, 201)
        paymentOrderId = created.body.paymentOrderId!
        const authorized = await managerApi.payments.authorize(
          merchantAlphaId,
          paymentOrderId,
          etagOf((await managerApi.payments.get(merchantAlphaId, paymentOrderId)).headers),
          uniqueIdempotencyKey(testInfo, 'OPS120-A'),
        )
        expectStatus(authorized, 200)
      })

      const wsPromise = await test.step('open the operations feed and subscribe to live events', async () => {
        const socket = app.overview.opsFeed.attachWebSocket()
        await app.overview.goto()
        await app.overview.opsFeed.expectLoaded()
        return socket
      })

      await test.step('capture the payment and observe its live feed entry', async () => {
        const ws = await wsPromise
        const framePromise = ws.waitForEvent('framereceived')
        const captured = await managerApi.payments.capture(
          merchantAlphaId,
          paymentOrderId!,
          etagOf((await managerApi.payments.get(merchantAlphaId, paymentOrderId!)).headers),
          uniqueIdempotencyKey(testInfo, 'OPS120-P'),
          1500,
        )
        expectStatus(captured, 200)
        const frame = await framePromise
        expect(String(frame.payload)).toContain('PAYMENT_CAPTURED')
        await app.overview.opsFeed.waitForOpsEvent({ orderRef: reference, type: 'CAPTURED' })
      })
  })

  test('PW-OPS-E2E-121 duplicate eventId is one row', async ({ app, api }) => {
    const client = api
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
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
    await wsPromise
    expect((await client.operations.injectFeed(payload)).status).toBe(201)
    expect((await client.operations.injectFeed(payload)).status).toBe(201)
    await app.overview.opsFeed.waitForOpsEvent({ eventId })
    await expect(app.overview.opsFeed.rowByEventId(eventId)).toHaveCount(1)
  })

  test('PW-OPS-E2E-122 out-of-order frames sort by occurredAt', async ({ app, api }) => {
    const client = api
    const laterId = randomUUID()
    const earlierId = randomUUID()
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
    await wsPromise
    expect((await client.operations.injectFeed({
      eventId: laterId,
      type: 'PAYMENT_CAPTURED',
      label: 'PO-122-T2  CAPTURED',
      occurredAt: '2026-08-20T10:42:03Z',
    })).status).toBe(201)
    expect((await client.operations.injectFeed({
      eventId: earlierId,
      type: 'PAYMENT_AUTHORIZED',
      label: 'PO-122-T1  AUTHORIZED',
      occurredAt: '2026-08-20T10:41:01Z',
    })).status).toBe(201)
    await app.overview.opsFeed.waitForOpsEvent({ eventId: laterId })
    await app.overview.opsFeed.waitForOpsEvent({ eventId: earlierId })
    const labels = await app.overview.opsFeed.rowLabels().allTextContents()
    const earlierIndex = labels.findIndex(text => text.includes('PO-122-T1'))
    const laterIndex = labels.findIndex(text => text.includes('PO-122-T2'))
    expect(earlierIndex).toBeGreaterThanOrEqual(0)
    expect(laterIndex).toBeGreaterThan(earlierIndex)
  })

  test('PW-OPS-E2E-123 malformed toast still connected', async ({ app, api }) => {
    const client = api
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
    await wsPromise
    expect((await client.operations.injectFeed({ raw: '{not-json' })).status).toBe(201)
    await expect(app.overview.opsFeed.invalidEventToast()).toBeVisible()
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
  })

  test('PW-OPS-E2E-124 setOffline flips connected chip', async ({ app, page }) => {
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
    await wsPromise
    await page.context().setOffline(true)
    await expect(app.overview.opsFeed.chip()).toHaveText('disconnected')
    await page.context().setOffline(false)
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
  })

  test('PW-OPS-E2E-120…125 401 handshake → disconnected chip, no reconnect', async ({
    app,
    page,
  }) => {
    const firstWs = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
    await firstWs

    const deniedWs = app.overview.opsFeed.attachWebSocket()
    await page.context().clearCookies()
    await page.context().setOffline(true)
    await expect(app.overview.opsFeed.chip()).toHaveText('disconnected')
    await page.context().setOffline(false)
    const socket = await deniedWs
    await socket.waitForEvent('close')
    await expect(app.overview.opsFeed.chip()).toHaveText('disconnected')

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
    const client = api
    const eventId = randomUUID()
    const wsPromise = app.overview.opsFeed.attachWebSocket()
    await app.overview.goto()
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
    await wsPromise
    expect((await client.operations.injectFeed({
      eventId,
      type: 'PAYMENT_CAPTURED',
      label: 'PO-125  CAPTURED',
      occurredAt: '2026-08-20T10:42:03Z',
    })).status).toBe(201)
    await app.overview.opsFeed.waitForOpsEvent({ eventId })
    await page.context().setOffline(true)
    await expect(app.overview.opsFeed.chip()).toHaveText('disconnected')
    await page.context().setOffline(false)
    await expect(app.overview.opsFeed.chip()).toHaveText('connected')
    await expect(app.overview.opsFeed.rowByEventId(eventId)).toHaveCount(1)
  })
})
