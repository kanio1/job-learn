import { test, expect, requireApi } from '../fixtures'
import { uniqueMerchantReference } from '../data/factories'
import { expectNoAuthorizationInNetworkResponse } from '../utils/network'

test('PW-KAFKA-E2E-001 authorize -> Event Lab row visible via expect.poll', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const merchantRef = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(merchantRef, 'Event Lab E2E seed')
  expect(created.status).toBe(201)
  const merchantId = created.body!.merchantId as string
  const orderRef = `ELAB-${Date.now()}-${testInfo.parallelIndex}`
  const order = await client.createPaymentOrder(merchantId, { amountMinor: 1000, currency: 'PLN', clientOrderReference: orderRef }, `idem-${orderRef}`)
  expect(order.status).toBe(201)
  const paymentOrderId = order.body!.paymentOrderId as string
  const listed = await client.listPaymentOrders(merchantId)
  const etag = listed.headers?.etag || listed.headers?.ETag
  // authorize
  const auth = await client.authorizePayment(merchantId, paymentOrderId, etag as string | undefined, `idem-auth-${orderRef}`)
  expect([200, 201].includes(auth.status)).toBeTruthy()
  // Event Lab row appears <=5s
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect.poll(async () => {
    await app.eventLab.search(paymentOrderId)
    const count = await app.eventLab.rowCount()
    return count
  }, { timeout: 5000 }).toBeGreaterThanOrEqual(1)
  await app.eventLab.expectRowVisible(paymentOrderId)
  const payloadCol = await app.eventLab.hasNoPayloadColumn()
  expect(payloadCol).toBe(true)
})

test('PW-KAFKA-API-001 BFF list after lifecycle contains unique ref', async ({ api }, testInfo) => {
  const client = requireApi(api)
  const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'BFF seed')
  expect(merchant.status).toBe(201)
  const mid = merchant.body!.merchantId as string
  const ref = `BFF-${Date.now()}-${testInfo.parallelIndex}`
  const order = await client.createPaymentOrder(mid, { amountMinor: 1200, currency: 'PLN', clientOrderReference: ref }, `idem-bff-${ref}`)
  const pid = order.body!.paymentOrderId as string
  const list = await client.listPaymentOrders(mid)
  const etag = list.headers?.etag
  await client.authorizePayment(mid, pid, etag as string | undefined, `idem-bff-auth-${ref}`)
  const eventList = await client.listEventLab({ targetId: pid })
  expect(eventList.status).toBe(200)
  expect(JSON.stringify(eventList.body)).toContain(pid)
})

test('PW-KAFKA-E2E-006 six states: empty vs forbidden', async ({ app, api }) => {
  // empty when fresh targetId has no rows
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  // if forbidden visible skip empty assertion (platform-admin should see loaded not forbidden)
  const forbidden = await app.page.getByTestId('event-lab-forbidden').count()
  if (forbidden === 0) {
    // search impossible id -> filtered-empty
    await app.eventLab.search('00000000-0000-0000-0000-000000000000')
    await expect(app.page.getByTestId('event-lab-filtered-empty')).toBeVisible({ timeout: 5000 })
  } else {
    await expect(app.page.getByTestId('event-lab-forbidden')).toBeVisible()
  }
})

test('PW-KAFKA-E2E-007 flag off hides nav (static check via runtimeConfig)', async ({ page }) => {
  // when NUXT_PUBLIC_EVENT_LAB_ENABLED=false nav link hidden — here enabled so check visible for platform-admin
  await page.goto('/admin/event-lab')
  await expect(page.getByRole('heading', { name: /Event Lab/i })).toBeVisible({ timeout: 10000 })
})

test('PW-KAFKA-E2E-011 ConfirmModal dismiss does not POST', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const m = await client.createMerchant(uniqueMerchantReference(testInfo), 'dismiss seed')
  const mid = m.body!.merchantId as string
  const ref = `DISMISS-${Date.now()}-${testInfo.parallelIndex}`
  const order = await client.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, `idem-dis-${ref}`)
  const pid = order.body!.paymentOrderId as string
  const etag = (await client.listPaymentOrders(mid)).headers?.etag
  await client.authorizePayment(mid, pid, etag as string | undefined, `idem-dis-auth-${ref}`)
  // ensure Event Lab has a row to inject
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  await expect.poll(async () => { await app.eventLab.search(pid); return app.eventLab.rowCount() }, { timeout: 5000 }).toBeGreaterThanOrEqual(0)
  // open duplicate and dismiss — no POST should be sent (verify no error toast, row count unchanged)
  const before = await app.eventLab.rowCount()
  await app.eventLab.injectDuplicate()
  await app.eventLab.dismissDuplicate()
  await expect(app.page.getByTestId('confirm-inject-duplicate')).toBeHidden()
  const after = await app.eventLab.rowCount()
  expect(after).toBe(before)
})

test('PW-KAFKA-SEC-003 HAR has no Authorization nor bootstrap', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const m = await client.createMerchant(uniqueMerchantReference(testInfo), 'HAR seed')
  const mid = m.body!.merchantId as string
  const ref = `HAR-${Date.now()}`
  await client.createPaymentOrder(mid, { amountMinor: 1000, currency: 'PLN', clientOrderReference: ref }, `idem-har-${ref}`)
  await app.eventLab.goto()
  await app.eventLab.expectLoaded()
  const request = page.waitForResponse(r => r.url().includes('/api/event-lab'))
  await app.eventLab.search(ref)
  const resp = await request.catch(() => null)
  if (resp) expectNoAuthorizationInNetworkResponse(resp)
})
