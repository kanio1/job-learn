import { randomUUID } from 'node:crypto'
import type { TestInfo } from '@playwright/test'
import { test, expect } from '../fixtures'
import type { WorkerWorld } from '../fixtures'
import { requireEtag } from '../utils/http'
import { expectStatus } from '../api/bff-client'
import type { EventLabListRow } from '../api/bff-client'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import type { ProblemDetails } from '../utils/problem'
import { observeRequests } from '../utils/network-observer'

/**
 * Arrange one owned order and authorize it so the Event Lab receives the
 * auditable lifecycle event. Returns only the ids the spec needs; the arrival
 * of the Kafka row is a business oracle and stays in each spec's `expect.poll`.
 */
async function createAuthorizedEvent(
  workerWorld: WorkerWorld,
  ownedMerchantId: string,
  testInfo: TestInfo,
  tag: string,
  amountMinor = 1000,
): Promise<{ merchantId: string, paymentOrderId: string }> {
  const order = await workerWorld.api.payments.createOrder(
    ownedMerchantId,
    { amountMinor, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, tag) },
    uniqueIdempotencyKey(testInfo, tag),
  )
  expectStatus(order, 201)
  const paymentOrderId = order.body.paymentOrderId
  const etag = requireEtag((await workerWorld.api.payments.get(ownedMerchantId, paymentOrderId)).headers)
  const auth = await workerWorld.api.payments.authorize(
    ownedMerchantId,
    paymentOrderId,
    etag,
    uniqueIdempotencyKey(testInfo, `${tag}-AUTH`),
  )
  expect([200, 201].includes(auth.status)).toBeTruthy()
  return { merchantId: ownedMerchantId, paymentOrderId }
}

/** The first Event Lab row for a target — asserts presence, never hides it. */
function firstEventRow(body: EventLabListRow[] | ProblemDetails | undefined, targetId: string): EventLabListRow {
  const row = Array.isArray(body) ? body.find(candidate => candidate.targetId === targetId) : undefined
  if (row) {
    return row
  }
  throw new Error(`Event Lab list for ${targetId} must contain at least one row`)
}

function targetRowCount(body: EventLabListRow[] | ProblemDetails | undefined, targetId: string): number {
  return Array.isArray(body) ? body.filter(row => row.targetId === targetId).length : 0
}

test('PW-KAFKA-E2E-001 authorize -> Event Lab row visible via expect.poll', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await test.step('arrange an owned order and authorize it', () =>
    createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'ELAB'))
  await test.step('observe the Kafka event arrive over the BFF', async () => {
    await expect.poll(async () => {
      const list = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
      if (list.status !== 200) return 0
      return targetRowCount(list.body, paymentOrderId)
    }, { timeout: 5000 }).toBe(1)
  })
  await test.step('verify the Event Lab UI shows the row without a payload column', async () => {
    await app.eventLab.goto()
    await app.eventLab.expectLoaded()
    await expect.poll(async () => {
      await app.eventLab.search(paymentOrderId)
      return app.eventLab.rowCount()
    }, { timeout: 5000 }).toBeGreaterThanOrEqual(1)
    await expect(app.eventLab.eventRow(paymentOrderId)).toBeVisible()
    await expect(app.eventLab.payloadColumn()).toHaveCount(0)
  })
})

test('PW-KAFKA-API-001 BFF list after lifecycle contains unique ref', async ({ api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'BFF', 1200)
  await expect.poll(async () => {
    const eventList = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
    if (eventList.status !== 200) return 0
    return targetRowCount(eventList.body, paymentOrderId)
  }, { timeout: 5000 }).toBe(1)
  const eventList = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
  expectStatus(eventList, 200)
  expect(targetRowCount(eventList.body, paymentOrderId)).toBeGreaterThanOrEqual(1)
})

test('PW-KAFKA-E2E-006a empty state (no rows)', async ({ app }) => {
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect(app.eventLab.loading()).toBeHidden()
  await app.eventLab.search('00000000-0000-0000-0000-000000000000')
  await expect(app.eventLab.filteredEmpty()).toBeVisible({ timeout: 5000 })
  await expect(app.eventLab.error()).toHaveCount(0)
  await expect(app.eventLab.notFound()).toBeHidden()
})

test('PW-KAFKA-E2E-006b forbidden state (no read authority)', async ({ actors }) => {
  const { app } = await actors.open('merchantManager')
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await app.eventLab.expectForbidden()
  await expect(app.eventLab.error()).toHaveCount(0)
})

test('PW-KAFKA-E2E-007 Event Lab heading visible when flag enabled', async ({ app }) => {
  // Flag-on (--kafka): the Event Lab page is reachable and shows its heading.
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect(app.eventLab.heading()).toBeVisible()
})

test('PW-KAFKA-E2E-011 ConfirmModal dismiss does not POST', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await test.step('arrange an owned order and authorize it', () =>
    createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'DIS'))
  await test.step('observe the Kafka event arrive over the BFF', async () => {
    await expect.poll(async () => {
      const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
      if (ev.status !== 200) return 0
      return targetRowCount(ev.body, paymentOrderId)
    }, { timeout: 5000 }).toBe(1)
  })
  await test.step('dismiss the duplicate confirm modal and assert no POST is emitted', async () => {
    await app.eventLab.goto()
    await app.eventLab.expectLoaded()
    await expect.poll(async () => {
      await app.eventLab.search(paymentOrderId)
      return app.eventLab.rowCount()
    }, { timeout: 5000 }).toBeGreaterThanOrEqual(1)
    const { requests } = await observeRequests(
      app.page,
      request => request.method() === 'POST' && request.url().includes('/api/event-lab/inject/duplicate'),
      async () => {
        await app.eventLab.injectDuplicate()
        await app.eventLab.dismissDuplicate()
        await expect(app.eventLab.duplicateConfirmation()).toBeHidden()
      },
    )
    expect(requests).toHaveLength(0)
  })
})

test('PW-KAFKA-SEC-003 no Authorization header or Kafka bootstrap in browser requests', async ({ app, workerWorld, ownedMerchantId }, testInfo) => {
  await test.step('arrange an owned order', async () => {
    const order = await workerWorld.api.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1000, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'HAR') },
      uniqueIdempotencyKey(testInfo, 'HAR'),
    )
    expectStatus(order, 201)
  })
  await test.step('open Event Lab and observe its browser request window', async () => {
    const { requests } = await observeRequests(app.page, () => true, async () => {
    await app.eventLab.goto()
    await app.eventLab.expectLoaded()
    // search is client-side: still no auth/kafka leak
    await app.eventLab.search(uniqueOrderReference(testInfo, 'HAR'))
    await expect(app.eventLab.settledListState()).toBeVisible({ timeout: 5000 })
    })
    const hasAuthLeak = requests.some(request => {
      const headers = request.headers()
      return request.url().includes('/api/event-lab') && Boolean(headers.authorization)
    })
    const hasKafkaLeak = requests.some(request => /bootstrap|kafka|9092|9091/.test(request.url()))
    expect(hasAuthLeak).toBe(false)
    expect(hasKafkaLeak).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// BFF negatives (API-002 .. 007)
// ---------------------------------------------------------------------------

test('PW-KAFKA-API-002 list without event-lab:read →403', async ({ actors }) => {
  const ro = await actors.open('readOnlyUser')
  expectStatus(await ro.api.labs.listEventLab(), 403)
})

test('PW-KAFKA-API-003 detail unknown id →404', async ({ api }) => {
  const adminClient = api
  const res = await adminClient.labs.getEventLabDetail(randomUUID())
  expectStatus(res, 404)
})

test('PW-KAFKA-API-004 guest no session →401 on /api/event-lab', async ({ actors }) => {
  const guest = await actors.open('guest')
  expectStatus(await guest.api.labs.listEventLab(), 401)
})

test('PW-KAFKA-API-005 inject without operate →403', async ({ api, actors, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'NOOP')
  await expect.poll(async () => {
    const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
    if (ev.status !== 200) return 0
    return targetRowCount(ev.body, paymentOrderId)
  }, { timeout: 5000 }).toBe(1)
  const list = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
  const eventId = firstEventRow(list.body, paymentOrderId).eventId
  const ro = await actors.open('readOnlyUser')
  expectStatus(await ro.api.labs.injectDuplicate(eventId), 403)
})

test('PW-KAFKA-API-006 duplicate inject →201 still 1 row', async ({ api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'DUP201')
  await expect.poll(async () => {
    const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
    if (ev.status !== 200) return 0
    return Array.isArray(ev.body) ? ev.body.length : 0
  }, { timeout: 5000 }).toBe(1)
  const list = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
  const eventId = firstEventRow(list.body, paymentOrderId).eventId
  const dup = await adminClient.labs.injectDuplicate(eventId)
  expectStatus(dup, 201)
  await expect.poll(async () => {
    const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
    if (ev.status !== 200) return 0
    return Array.isArray(ev.body) ? ev.body.length : 0
  }, { timeout: 5000 }).toBe(1)
})

test('PW-KAFKA-API-007 unknown query param →400', async ({ api }) => {
  expectStatus(await api.labs.listEventLab({ unknown: '1' }), 400)
})

// ---------------------------------------------------------------------------
// E2E UI (002 .. 005, 008 .. 010, 012 .. 014)
// ---------------------------------------------------------------------------

test('PW-KAFKA-E2E-002 authorize -> payment detail delivery card PROCESSED', async ({ app, workerWorld, ownedMerchantId }, testInfo) => {
  const { paymentOrderId } = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'E2E002')

  // Visit the payment order detail page: the Delivery Proof card must reach PROCESSED.
  await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.deliveryCard()).toBeVisible({ timeout: 5000 })
  await expect(app.paymentDetail.deliveryProcessed()).toBeVisible({ timeout: 10000 })
  await expect(app.paymentDetail.deliveryStatus()).toContainText('PROCESSED')
})

test('PW-KAFKA-E2E-003 search paymentOrderId -> 1 row', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const first = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'SRCH1')
  const second = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'SRCH2', 1100)
  await expect.poll(async () => {
    const a = await adminClient.labs.listEventLab({ targetId: first.paymentOrderId })
    const b = await adminClient.labs.listEventLab({ targetId: second.paymentOrderId })
    if (a.status !== 200 || b.status !== 200) return 0
    const okA = targetRowCount(a.body, first.paymentOrderId)
    const okB = targetRowCount(b.body, second.paymentOrderId)
    return okA + okB
  }, { timeout: 5000 }).toBe(2)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await app.eventLab.search(first.paymentOrderId)
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBe(1)
  await expect(app.eventLab.eventRow(first.paymentOrderId)).toBeVisible()
})

test('PW-KAFKA-E2E-004 inject duplicate + confirm -> still 1 row', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await test.step('arrange an owned order and observe its single event row', async () => {
    const event = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'E004')
    const { paymentOrderId } = event
    await expect.poll(async () => {
      const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
      if (ev.status !== 200) return 0
      return Array.isArray(ev.body) ? ev.body.length : 0
    }, { timeout: 5000 }).toBe(1)
    return event
  })
  await test.step('confirm the duplicate inject and assert the row count stays 1', async () => {
    await app.eventLab.goto()
    await app.eventLab.expectLoaded()
    await expect.poll(async () => {
      await app.eventLab.search(paymentOrderId)
      return app.eventLab.rowCount()
    }, { timeout: 5000 }).toBe(1)
    await app.eventLab.injectDuplicate()
    await app.eventLab.confirmDuplicate()
    await expect(app.eventLab.duplicateConfirmation()).toBeHidden({ timeout: 5000 })
    await expect.poll(async () => {
      const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
      if (ev.status !== 200) return 0
      return Array.isArray(ev.body) ? ev.body.length : 0
    }, { timeout: 5000 }).toBe(1)
    await app.eventLab.search(paymentOrderId)
    await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBe(1)
  })
})

test('PW-KAFKA-E2E-005 inject poison -> DEAD + banner DLT, payment status unchanged', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId, beforeStatus } = await test.step('arrange an owned order and observe its single event row', async () => {
    const event = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'P5')
    const { paymentOrderId } = event
    await expect.poll(async () => {
      const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
      if (ev.status !== 200) return 0
      return Array.isArray(ev.body) ? ev.body.length : 0
    }, { timeout: 5000 }).toBe(1)
    const before = await workerWorld.api.payments.get(ownedMerchantId, paymentOrderId)
    return { paymentOrderId, beforeStatus: before.body?.status }
  })
  await test.step('inject the poison event and observe DEAD plus the DLT banner', async () => {
    const list = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
    const eventId = firstEventRow(list.body, paymentOrderId).eventId
    const poison = await adminClient.labs.injectPoison(eventId)
    expectStatus(poison, 201)
    await expect.poll(async () => {
      const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
      if (ev.status !== 200) return ''
      return Array.isArray(ev.body)
        && ev.body.some(row => row.targetId === paymentOrderId && row.status === 'DEAD')
    }, { timeout: 5000 }).toBe(true)
    await app.eventLab.goto()
    await app.eventLab.expectLoaded()
    await app.eventLab.search(paymentOrderId)
    await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBeGreaterThanOrEqual(1)
    await expect(app.eventLab.dltBanner()).toBeVisible()
  })
  await test.step('assert the payment status is unchanged by the poison', async () => {
    const after = await workerWorld.api.payments.get(ownedMerchantId, paymentOrderId)
    expect(after.body?.status).toBe(beforeStatus)
  })
})

test('PW-KAFKA-E2E-008 two different orders -> two rows', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const first = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'TWOA')
  const second = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'TWOB')
  await expect.poll(async () => {
    const a = await adminClient.labs.listEventLab({ targetId: first.paymentOrderId })
    const b = await adminClient.labs.listEventLab({ targetId: second.paymentOrderId })
    if (a.status !== 200 || b.status !== 200) return 0
    const okA = targetRowCount(a.body, first.paymentOrderId)
    const okB = targetRowCount(b.body, second.paymentOrderId)
    return okA + okB
  }, { timeout: 5000 }).toBe(2)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBeGreaterThanOrEqual(2)
  await expect(app.eventLab.payloadColumn()).toHaveCount(0)
})

test('PW-KAFKA-E2E-009 deep-link detail existing', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'DEEP')
  await expect.poll(async () => {
    const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
    if (ev.status !== 200) return 0
    return Array.isArray(ev.body) ? ev.body.length : 0
  }, { timeout: 5000 }).toBe(1)
  const list = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
  const id = firstEventRow(list.body, paymentOrderId).id
  await app.eventLab.gotoDetail(id)
  await app.eventLab.expectLoaded()
  await expect(app.eventLab.notFound()).toBeHidden()
  // table should still be visible (deep-link does not hide list)
  await expect(app.eventLab.settledListState()).toBeVisible({ timeout: 5000 })
})

test('PW-KAFKA-E2E-010 deep-link bad id -> not-found', async ({ app }) => {
  const bad = randomUUID()
  await app.eventLab.gotoDetail(bad)
  await app.eventLab.expectLoaded()
  await expect.poll(async () => await app.eventLab.notFound().count(), { timeout: 5000 }).toBe(1)
  await expect(app.eventLab.notFound()).toBeVisible()
})

test('PW-KAFKA-E2E-012 delivery card pending before listener then PROCESSED', async ({ app, workerWorld, ownedMerchantId }, testInfo) => {
  const paymentOrderId = await test.step('arrange an owned order without authorizing', async () => {
    const order = await workerWorld.api.payments.createOrder(
      ownedMerchantId,
      { amountMinor: 1000, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'PEND') },
      uniqueIdempotencyKey(testInfo, 'PEND'),
    )
    expectStatus(order, 201)
    const paymentOrderId = order.body.paymentOrderId

    // Before the lifecycle event, the Delivery Proof card must be visible and in
    // its pending (waiting for the inspector ≤5s) or processed state — never error.
    await app.paymentDetail.gotoOrder(ownedMerchantId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.paymentDetail.deliveryCard()).toBeVisible({ timeout: 5000 })
    await expect(app.paymentDetail.deliveryState()).toBeVisible({ timeout: 5000 })
    return paymentOrderId
  })
  await test.step('authorize and wait for the card to reach PROCESSED', async () => {
    const etag = requireEtag((await workerWorld.api.payments.get(ownedMerchantId, paymentOrderId)).headers)
    const auth = await workerWorld.api.payments.authorize(
      ownedMerchantId,
      paymentOrderId,
      etag,
      uniqueIdempotencyKey(testInfo, 'PEND-A'),
    )
    expect([200, 201].includes(auth.status)).toBeTruthy()

    // The card transitions to PROCESSED via the real delivery path.
    await expect(app.paymentDetail.deliveryProcessed()).toBeVisible({ timeout: 10000 })
    await expect(app.paymentDetail.deliveryStatus()).toContainText('PROCESSED')
  })
})

test('PW-KAFKA-E2E-013 merchant manager forbidden', async ({ actors }) => {
  const { app: managerApp } = await actors.open('merchantManager')
  await managerApp.eventLab.goto()
  await managerApp.eventLab.expectLoaded()
  await managerApp.eventLab.expectForbidden()
  await expect(managerApp.sidebar.eventLab()).toHaveCount(0)
})

test('PW-KAFKA-E2E-014 no raw payload column', async ({ app, api, workerWorld, ownedMerchantId }, testInfo) => {
  const adminClient = api
  const { paymentOrderId } = await createAuthorizedEvent(workerWorld, ownedMerchantId, testInfo, 'NOPAY')
  await expect.poll(async () => {
    const ev = await adminClient.labs.listEventLab({ targetId: paymentOrderId })
    if (ev.status !== 200) return 0
    return targetRowCount(ev.body, paymentOrderId)
  }, { timeout: 5000 }).toBe(1)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await app.eventLab.search(paymentOrderId)
  await expect.poll(async () => await app.eventLab.rowCount(), { timeout: 5000 }).toBeGreaterThanOrEqual(1)
  await expect(app.eventLab.payloadColumn()).toHaveCount(0)
})
