import { uniqueIdempotencyKey, uniqueMerchantReference, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { waitForBffResponse } from '../utils/wait-bff'
import { observeRequests } from '../utils/network-observer'
import { merchant360Journey } from '../methods/use-case/Merchant360Journey'
import { etagOf } from '../utils/http'
import { expectStatus } from '../api/bff-client'
import { workerMerchant, POM_WORKER_COUNT } from '../auth/accounts'
import { workerManagerAuthFile } from '../utils/env'

test.describe('Merchant 360 slideover', { tag: ['@a11y'] }, () => {
  test('PW-M360-E2E-060 Open unique merchant stays on list and GETs detail', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const displayName = `360 ${reference}`
    const created = await client.merchants.create(reference, displayName)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!
    const detailPath = `/api/merchants/${merchantId}`

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()

    const detail = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
    await app.merchants.open360(reference)
    const response = await detail
    expect(response.status()).toBe(200)
    await app.merchantSlideover.expectOpen()
    await expect(app.page).toHaveURL(/\/admin\/merchants(\?|$)/)
    await expect(app.page).not.toHaveURL(new RegExp(`/admin/merchants/${merchantId}(?:\\?|$)`))
  })

  test('PW-M360-API-020 panel GET detail merchantId matches created row', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Api360 ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!
    const listed = await client.merchants.get(merchantId)
    expectStatus(listed, 200)
    expect(listed.body.merchantId).toBe(merchantId)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
    const detail = waitForBffResponse(page, { method: 'GET', pathExact: `/api/merchants/${merchantId}` })
    await app.merchants.open360(reference)
    const response = await detail
    expect(response.status()).toBe(200)
    const body = await response.json()
    expect(body).toEqual(expect.objectContaining({ merchantId }))
    await expect(app.merchantSlideover.dialog()).toBeVisible()
  })

  test('PW-M360-E2E-061 dialog ARIA snapshot has heading and close', async ({ app, api }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.merchants.create(reference, `Aria ${reference}`)).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()
    await expect(app.merchantSlideover.dialog()).toMatchAriaSnapshot(`
      - heading "Merchant 360"
      - button "Close"
    `)
  })

  test('PW-M360-E2E-062 Escape hides dialog and returns focus to the row', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.merchants.create(reference, `Esc ${reference}`)).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
    const trigger = app.merchants.openButton(reference)
    await trigger.click()
    await app.merchantSlideover.expectOpen()
    await app.merchantSlideover.closeWithEscape()
    await app.merchantSlideover.expectClosed()
    await expect(trigger).toBeFocused()
    await expect(page).toHaveURL(/\/admin\/merchants/)
  })

  test('PW-M360-E2E-063 Confirm suspend dismiss does not POST', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Susp ${reference}`)
    expectStatus(created, 201)
    expect((await client.merchants.activate(created.body.merchantId!, etagOf(created.headers)!)).status).toBe(200)
    const suspendPath = `/api/merchants/${created.body.merchantId}/suspend`

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()

    const { requests } = await observeRequests(page, (request) => {
      try {
        return request.method() === 'POST' && new URL(request.url()).pathname === suspendPath
      }
      catch {
        return false
      }
    }, async () => {
      await app.merchantSlideover.openSuspendConfirm()
      await app.merchantSlideover.confirm.dismiss()
      await expect(app.merchantSlideover.confirm.heading('Suspend merchant')).toHaveCount(0)
      await expect(app.merchantSlideover.dialog()).toBeVisible()
    })
    expect(requests).toHaveLength(0)
    expect(merchant360Journey.nestedSuspendDismiss.confirmDismissMustNotPost).toBe('/suspend')
  })

  test('PW-M360-E2E-132 activate then 360 timeline contains ACTIVE', async ({ app, api }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Tl ${reference}`)
    expectStatus(created, 201)
    expect((await client.merchants.activate(created.body.merchantId!, etagOf(created.headers)!)).status).toBe(200)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()
    await expect(app.merchantSlideover.timeline()).toBeVisible()
    await expect(app.merchantSlideover.timeline()).toContainText('ACTIVE')
  })

  test('PW-M360-E2E-133-360 timeline includes payment history transitions', async ({
    app,
    actors,
  }, testInfo) => {
    const index = testInfo.parallelIndex % POM_WORKER_COUNT
    const world = workerMerchant(index)
    const managerApi = (await actors.openStorageState(workerManagerAuthFile(index))).api
      const reference = uniqueOrderReference(testInfo, '360H')
      const created = await managerApi.payments.createOrder(
        world.merchantId,
        { amountMinor: 1800, currency: 'PLN', clientOrderReference: reference },
        uniqueIdempotencyKey(testInfo, '360HC'),
      )
      expectStatus(created, 201)
      expect((await managerApi.payments.authorize(
        world.merchantId,
        created.body.paymentOrderId!,
        etagOf(created.headers),
        uniqueIdempotencyKey(testInfo, '360HA'),
      )).status).toBe(200)

      await app.merchants.goto()
      await app.merchants.expectLoaded()
      await app.merchants.filterByText(world.merchantReference)
      await app.merchants.applyFilters()
      await expect(app.merchants.rowCell(world.merchantReference)).toBeVisible()
      await app.merchants.open360(world.merchantReference)
      await app.merchantSlideover.expectOpen()
      await expect(app.merchantSlideover.sectionHeading('Notes')).toBeVisible()
      await expect(app.merchantSlideover.timeline()).toContainText('AUTHORIZED')
      await expect(app.merchantSlideover.timeline()).toContainText(reference)
  })

  test('PW-M360-E2E-064 registry current nav is Registry not Payment Orders', async ({ app }) => {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await expect(app.sidebar.merchants()).toHaveAttribute('aria-current', 'page')
    const paymentsNav = app.sidebar.paymentOrders()
    await expect(paymentsNav).toBeVisible()
    await expect(paymentsNav).not.toHaveAttribute('aria-current', 'page')
    await expect(paymentsNav).not.toHaveAttribute('href', '/admin/merchants')
  })

  test('PW-M360-E2E-065 360 View payment orders opens the merchant list', async ({ app, api }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `PayNav ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()
    await app.merchantSlideover.openPaymentOrders()
    await expect(app.page).toHaveURL(new RegExp(`/admin/merchants/${merchantId}/payments`))
    await expect(app.payments.heading()).toBeVisible()
    await expect(app.sidebar.paymentOrders()).toHaveAttribute('aria-current', 'page')
    await expect(app.sidebar.merchants()).not.toHaveAttribute('aria-current', 'page')
  })
})
