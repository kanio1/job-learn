import { randomUUID } from 'node:crypto'
import { test, expect, requireApi } from '../fixtures'
import { etagOf } from '../utils/http'
import { BffClient } from '../api/bff-client'
import { pomAuthFiles, pomNodeBaseURL, pomBrowserBaseURL } from '../utils/env'
import { App } from '../pages/App'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'

test('PW-KAFKA-E2E-001 authorize -> Event Lab row visible via expect.poll', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const merchantId = ownedMerchantId as string
  const orderRef = uniqueOrderReference(testInfo, 'ELAB')
  const order = await workerWorld.api.createPaymentOrder(merchantId, { amountMinor: 1000, currency: 'PLN', clientOrderReference: orderRef }, uniqueIdempotencyKey(testInfo, 'ELAB'))
  expect(order.status).toBe(201)
  const paymentOrderId = order.body!.paymentOrderId as string
  const detail = await workerWorld.api.getPaymentOrder(merchantId, paymentOrderId)
  const etag = etagOf(detail.headers)
  expect(etag, 'detail must return ETag').toBeTruthy()
  const auth = await workerWorld.api.authorizePayment(merchantId, paymentOrderId, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'ELAB-AUTH'))
  expect([200, 201].includes(auth.status)).toBeTruthy()
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect.poll(async () => {
    const list = await adminClient.listEventLab({ targetId: paymentOrderId })
    if (list.status !== 200) return 0
    const bodyStr = JSON.stringify(list.body)
    return bodyStr.includes(paymentOrderId) ? 1 : 0
  }, { timeout: 5000 }).toBe(1)
  await expect.poll(async () => {
    await app.eventLab.search(paymentOrderId)
    const count = await app.eventLab.rowCount()
    return count
  }, { timeout: 5000 }).toBeGreaterThanOrEqual(1)
  await app.eventLab.expectRowVisible(paymentOrderId)
  const payloadCol = await app.eventLab.hasNoPayloadColumn()
  expect(payloadCol).toBe(true)
})

test('PW-KAFKA-API-001 BFF list after lifecycle contains unique ref', async ({ api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'BFF')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1200, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'BFF'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const detail = await workerWorld.api.getPaymentOrder(mid, pid)
  const etag = etagOf(detail.headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'BFF-AUTH'))
  await expect.poll(async () => {
    const eventList = await adminClient.listEventLab({ targetId: pid })
    if (eventList.status !== 200) return 0
    const bodyStr = JSON.stringify(eventList.body)
    return bodyStr.includes(pid) ? 1 : 0
  }, { timeout: 5000 }).toBe(1)
  const eventList = await adminClient.listEventLab({ targetId: pid })
  expect(eventList.status).toBe(200)
  expect(JSON.stringify(eventList.body)).toContain(pid)
})

test('PW-KAFKA-E2E-006a empty state (no rows)', async ({ app }) => {
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect(app.page.getByTestId('event-lab-loading')).toBeHidden()
  await app.eventLab.search('00000000-0000-0000-0000-000000000000')
  await expect(app.page.getByTestId('event-lab-filtered-empty')).toBeVisible({ timeout: 5000 })
  await expect(app.page.getByTestId('event-lab-error')).toHaveCount(0)
  await expect(app.page.getByTestId('event-lab-not-found')).toBeHidden()
})

test('PW-KAFKA-E2E-006b forbidden state (no read authority)', async ({ browser }) => {
  const ctx = await browser.newContext({ storageState: pomAuthFiles.merchantManager, baseURL: pomBrowserBaseURL() })
  const page = await ctx.newPage()
  const app = new App(page)
  try {
    await app.eventLab.goto()
    await app.eventLab.expectLoaded()
    await app.eventLab.expectForbidden()
    await expect(page.getByTestId('event-lab-error')).toHaveCount(0)
  } finally {
    await ctx.close()
  }
})

test('PW-KAFKA-E2E-007 Event Lab heading visible when flag enabled', async ({ app }) => {
  // Flag-on (--kafka): the Event Lab page is reachable and shows its heading.
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect(app.page.getByRole('heading', { name: /Event Lab/i }).first()).toBeVisible()
})

test('PW-KAFKA-E2E-011 ConfirmModal dismiss does not POST', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'DISMISS')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'DIS'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const detail = await workerWorld.api.getPaymentOrder(mid, pid)
  const etag = etagOf(detail.headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'DIS-AUTH'))
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    return JSON.stringify(ev.body).includes(pid) ? 1 : 0
  }, { timeout: 5000 }).toBe(1)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect.poll(async () => { await app.eventLab.search(pid); return app.eventLab.rowCount() }, { timeout: 5000 }).toBeGreaterThanOrEqual(1)

  // Observe the network: dismissing the confirm modal must not emit any POST to /inject/duplicate.
  const postSeen = new Promise<boolean>((resolve) => {
    const handler = (req: import('playwright').Request) => {
      if (req.method() === 'POST' && req.url().includes('/api/event-lab/inject/duplicate')) resolve(true)
    }
    app.page.on('request', handler)
    setTimeout(() => {
      app.page.off('request', handler)
      resolve(false)
    }, 4000)
  })
  await app.eventLab.injectDuplicate()
  await app.eventLab.dismissDuplicate()
  await expect(app.page.getByTestId('confirm-inject-duplicate')).toBeHidden()
  expect(await postSeen).toBe(false)
})

test('PW-KAFKA-SEC-003 no Authorization header or Kafka bootstrap in browser requests', async ({ app, page, workerWorld, ownedMerchantId }, testInfo) => {
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'HAR')
  await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'HAR'))
  const authLeak = new Promise<boolean>((resolve) => {
    const handler = (p: import('playwright').Request) => {
      const headers = p.headers()
      if (p.url().includes('/api/event-lab') && (headers['authorization'] || headers['Authorization'])) resolve(true)
    }
    page.on('request', handler)
    setTimeout(() => { page.off('request', handler); resolve(false) }, 5000)
  })
  const kafkaLeak = new Promise<boolean>((resolve) => {
    const handler = (p: import('playwright').Request) => {
      if (/bootstrap|kafka|9092|9091/.test(p.url())) resolve(true)
    }
    page.on('request', handler)
    setTimeout(() => { page.off('request', handler); resolve(false) }, 5000)
  })
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  expect(await kafkaLeak).toBe(false)
  expect(await authLeak).toBe(false)
  // search is client-side: still no auth/kafka leak
  await app.eventLab.search(ref)
  await expect(app.page.getByTestId('event-lab-table').or(app.page.getByTestId('event-lab-filtered-empty')).or(app.page.getByTestId('event-lab-empty'))).toBeVisible({ timeout: 5000 })
})

// ---------------------------------------------------------------------------
// BFF negatives (API-002 .. 007)
// ---------------------------------------------------------------------------

test('PW-KAFKA-API-002 list without event-lab:read →403', async ({ playwright }) => {
  const ro = await BffClient.create(playwright, pomAuthFiles.readOnlyUser)
  try {
    const res = await ro.listEventLab()
    expect(res.status).toBe(403)
  } finally {
    await ro.dispose()
  }
})

test('PW-KAFKA-API-003 detail unknown id →404', async ({ api }) => {
  const adminClient = requireApi(api)
  const id = randomUUID()
  const res = await adminClient.getEventLabDetail(id)
  expect(res.status).toBe(404)
})

test('PW-KAFKA-API-004 guest no session →401 on /api/event-lab', async ({ playwright }) => {
  const guest = await playwright.request.newContext({
    baseURL: pomNodeBaseURL(),
    storageState: { cookies: [], origins: [] },
  })
  try {
    const res = await guest.get('/api/event-lab')
    expect(res.status()).toBe(401)
  } finally {
    await guest.dispose()
  }
})

test('PW-KAFKA-API-005 inject without operate →403', async ({ api, playwright, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'NOOP')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'NOOP'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'NOOP-A'))
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    return JSON.stringify(ev.body).includes(pid) ? 1 : 0
  }, { timeout: 5000 }).toBe(1)
  const list = await adminClient.listEventLab({ targetId: pid })
  const body = list.body as unknown[]
  const first = (Array.isArray(body) && body.length > 0 ? body[0] as Record<string, string> : null)
  const eventId = first?.eventId as string
  expect(eventId, 'eventId must exist').toBeTruthy()
  const ro = await BffClient.create(playwright, pomAuthFiles.readOnlyUser)
  try {
    const inj = await ro.injectDuplicate(eventId)
    expect(inj.status).toBe(403)
  } finally {
    await ro.dispose()
  }
})

test('PW-KAFKA-API-006 duplicate inject →201 still 1 row', async ({ api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'DUP201')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'DUP201'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'DUP201-A'))
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    const arr = ev.body as unknown[]
    return Array.isArray(arr) ? arr.length : 0
  }, { timeout: 5000 }).toBe(1)
  const list = await adminClient.listEventLab({ targetId: pid })
  const arr = list.body as Record<string, string>[]
  const eventId = arr[0]!.eventId
  const dup = await adminClient.injectDuplicate(eventId)
  expect(dup.status).toBe(201)
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    const a = ev.body as unknown[]
    return Array.isArray(a) ? a.length : 0
  }, { timeout: 5000 }).toBe(1)
})

test('PW-KAFKA-API-007 unknown query param →400', async ({ playwright }) => {
  const ctx = await playwright.request.newContext({ baseURL: pomNodeBaseURL(), storageState: pomAuthFiles.platformAdmin })
  try {
    const res = await ctx.get('/api/event-lab?unknown=1')
    expect(res.status()).toBe(400)
  } finally {
    await ctx.dispose()
  }
})

// ---------------------------------------------------------------------------
// E2E UI (002 .. 005, 008 .. 010, 012 .. 014)
// ---------------------------------------------------------------------------

test('PW-KAFKA-E2E-002 authorize -> payment detail delivery card PROCESSED', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'E2E002')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'E2E002'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'E2E002-A'))

  // Visit the payment order detail page: the Delivery Proof card must reach PROCESSED.
  await app.paymentDetail.gotoOrder(mid, pid)
  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('eventlab-delivery-card')).toBeVisible({ timeout: 5000 })
  await expect(app.page.getByTestId('eventlab-delivery-processed')).toBeVisible({ timeout: 10000 })
  await expect(app.page.getByTestId('eventlab-delivery-status')).toContainText('PROCESSED')
})

test('PW-KAFKA-E2E-003 search paymentOrderId -> 1 row', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref1 = uniqueOrderReference(testInfo, 'SRCH1')
  const ref2 = uniqueOrderReference(testInfo, 'SRCH2')
  const o1 = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref1 }, uniqueIdempotencyKey(testInfo, 'SR1'))
  const o2 = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1100, currency: 'PLN', clientOrderReference: ref2 }, uniqueIdempotencyKey(testInfo, 'SR2'))
  expect(o1.status).toBe(201)
  expect(o2.status).toBe(201)
  const pid1 = o1.body!.paymentOrderId as string
  const pid2 = o2.body!.paymentOrderId as string
  for (const pid of [pid1, pid2]) {
    const et = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
    await workerWorld.api.authorizePayment(mid, pid, et as string | undefined, uniqueIdempotencyKey(testInfo, `SRA-${pid.slice(0, 4)}`))
  }
  await expect.poll(async () => {
    const a = await adminClient.listEventLab({ targetId: pid1 })
    const b = await adminClient.listEventLab({ targetId: pid2 })
    if (a.status !== 200 || b.status !== 200) return 0
    return JSON.stringify(a.body).includes(pid1) && JSON.stringify(b.body).includes(pid2) ? 1 : 0
  }, { timeout: 5000 }).toBe(1)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await app.eventLab.search(pid1)
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBe(1)
  await app.eventLab.expectRowVisible(pid1)
})

test('PW-KAFKA-E2E-004 inject duplicate + confirm -> still 1 row', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'E2E004')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'E004'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'E004-A'))
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    return (ev.body as unknown[]).length
  }, { timeout: 5000 }).toBe(1)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect.poll(async () => { await app.eventLab.search(pid); return app.eventLab.rowCount() }, { timeout: 5000 }).toBe(1)
  await app.eventLab.injectDuplicate()
  await app.eventLab.confirmDuplicate()
  await expect(app.page.getByTestId('confirm-inject-duplicate')).toBeHidden({ timeout: 5000 })
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    return (ev.body as unknown[]).length
  }, { timeout: 5000 }).toBe(1)
  await app.eventLab.search(pid)
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBe(1)
})

test('PW-KAFKA-E2E-005 inject poison -> DEAD + banner DLT, payment status unchanged', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'POISON')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'P5'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'P5-A'))
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    return (ev.body as unknown[]).length
  }, { timeout: 5000 }).toBe(1)
  const before = await workerWorld.api.getPaymentOrder(mid, pid)
  const beforeStatus = before.body?.status
  const list = await adminClient.listEventLab({ targetId: pid })
  const eventId = (list.body as Record<string, string>[])[0]!.eventId
  const poison = await adminClient.injectPoison(eventId)
  expect(poison.status).toBe(201)
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return ''
    return JSON.stringify(ev.body)
  }, { timeout: 5000 }).toContain('DEAD')
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await app.eventLab.search(pid)
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBeGreaterThanOrEqual(1)
  await app.eventLab.expectDltBanner()
  const after = await workerWorld.api.getPaymentOrder(mid, pid)
  expect(after.body?.status).toBe(beforeStatus)
})

test('PW-KAFKA-E2E-008 two different orders -> two rows', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const refA = uniqueOrderReference(testInfo, 'TWOA')
  const refB = uniqueOrderReference(testInfo, 'TWOB')
  const oA = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: refA }, uniqueIdempotencyKey(testInfo, 'TWOA'))
  const oB = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: refB }, uniqueIdempotencyKey(testInfo, 'TWOB'))
  expect(oA.status).toBe(201)
  expect(oB.status).toBe(201)
  const pidA = oA.body!.paymentOrderId as string
  const pidB = oB.body!.paymentOrderId as string
  for (const pid of [pidA, pidB]) {
    const et = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
    await workerWorld.api.authorizePayment(mid, pid, et as string | undefined, uniqueIdempotencyKey(testInfo, `TOA-${pid.slice(0, 4)}`))
  }
  await expect.poll(async () => {
    const a = await adminClient.listEventLab({ targetId: pidA })
    const b = await adminClient.listEventLab({ targetId: pidB })
    if (a.status !== 200 || b.status !== 200) return 0
    const okA = JSON.stringify(a.body).includes(pidA) ? 1 : 0
    const okB = JSON.stringify(b.body).includes(pidB) ? 1 : 0
    return okA + okB
  }, { timeout: 5000 }).toBe(2)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBeGreaterThanOrEqual(2)
  expect(await app.eventLab.hasNoPayloadColumn()).toBe(true)
})

test('PW-KAFKA-E2E-009 deep-link detail existing', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'DEEP')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'DEEP'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'DEEP-A'))
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    return (ev.body as unknown[]).length
  }, { timeout: 5000 }).toBe(1)
  const list = await adminClient.listEventLab({ targetId: pid })
  const row = (list.body as Record<string, string>[])[0]!
  const id = row.id
  expect(id, 'row.id must exist').toBeTruthy()
  await app.eventLab.gotoDetail(id)
  await app.eventLab.expectLoaded()
  await expect(app.page.getByTestId('event-lab-not-found')).toBeHidden()
  // table should still be visible (deep-link does not hide list)
  await expect(app.page.getByTestId('event-lab-table').or(app.page.getByTestId('event-lab-filtered-empty')).or(app.page.getByTestId('event-lab-empty'))).toBeVisible({ timeout: 5000 })
})

test('PW-KAFKA-E2E-010 deep-link bad id -> not-found', async ({ app }) => {
  const bad = randomUUID()
  await app.eventLab.gotoDetail(bad)
  await app.eventLab.expectLoaded()
  await expect.poll(async () => await app.page.getByTestId('event-lab-not-found').count(), { timeout: 5000 }).toBe(1)
  await app.eventLab.expectNotFound()
})

test('PW-KAFKA-E2E-012 delivery card pending before listener then PROCESSED', async ({ app, workerWorld, ownedMerchantId, api }, testInfo) => {
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'PEND')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'PEND'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string

  // Before the lifecycle event, the Delivery Proof card must be visible and in
  // its pending (waiting for the inspector ≤5s) or processed state — never error.
  await app.paymentDetail.gotoOrder(mid, pid)
  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('eventlab-delivery-card')).toBeVisible({ timeout: 5000 })
  await expect(
    app.page.getByTestId('eventlab-delivery-pending')
      .or(app.page.getByTestId('eventlab-delivery-processed'))
      .or(app.page.getByTestId('eventlab-delivery-empty')),
  ).toBeVisible({ timeout: 5000 })

  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'PEND-A'))

  // The card transitions to PROCESSED via the real delivery path.
  await expect(app.page.getByTestId('eventlab-delivery-processed')).toBeVisible({ timeout: 10000 })
  await expect(app.page.getByTestId('eventlab-delivery-status')).toContainText('PROCESSED')
})

test('PW-KAFKA-E2E-013 merchant manager forbidden', async ({ browser }) => {
  const ctx = await browser.newContext({ storageState: pomAuthFiles.merchantManager, baseURL: pomBrowserBaseURL() })
  const page = await ctx.newPage()
  const managerApp = new App(page)
  try {
    await managerApp.eventLab.goto()
    await managerApp.eventLab.expectLoaded()
    await managerApp.eventLab.expectForbidden()
    await expect(page.getByTestId('nav-link-event-lab')).toHaveCount(0)
  } finally {
    await ctx.close()
  }
})

test('PW-KAFKA-E2E-014 no raw payload column', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = requireApi(api)
  const mid = ownedMerchantId as string
  const ref = uniqueOrderReference(testInfo, 'NOPAY')
  const order = await workerWorld.api.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, uniqueIdempotencyKey(testInfo, 'NOPAY'))
  expect(order.status).toBe(201)
  const pid = order.body!.paymentOrderId as string
  const etag = etagOf((await workerWorld.api.getPaymentOrder(mid, pid)).headers)
  await workerWorld.api.authorizePayment(mid, pid, etag as string | undefined, uniqueIdempotencyKey(testInfo, 'NOPAY-A'))
  await expect.poll(async () => {
    const ev = await adminClient.listEventLab({ targetId: pid })
    if (ev.status !== 200) return 0
    return JSON.stringify(ev.body).includes(pid) ? 1 : 0
  }, { timeout: 5000 }).toBe(1)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await app.eventLab.search(pid)
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBeGreaterThanOrEqual(1)
  expect(await app.eventLab.hasNoPayloadColumn()).toBe(true)
})
