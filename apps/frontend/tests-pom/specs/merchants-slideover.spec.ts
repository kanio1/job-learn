import { uniqueIdempotencyKey, uniqueMerchantReference, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { waitForBffResponse } from '../utils/wait-bff'
import { merchant360Journey } from '../methods/use-case/Merchant360Journey'
import { etagOf } from '../utils/http'
import { BffClient , expectStatus } from '../api/bff-client'
import { workerMerchant, POM_WORKER_COUNT } from '../auth/accounts'
import { workerManagerAuthFile } from '../utils/env'

test.describe('Merchant 360 slideover', { tag: ['@a11y'] }, () => {
  test('PW-M360-E2E-060 Open unique merchant stays on list and GETs detail', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const displayName = `360 ${reference}`
    const created = await client.createMerchant(reference, displayName)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!
    const detailPath = `/api/merchants/${merchantId}`

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)

    const detail = waitForBffResponse(page, { method: 'GET', pathExact: detailPath })
    await app.merchants.open360(reference)
    const response = await detail
    expect(response.status()).toBe(200)
    await app.merchantSlideover.expectOpen()
    await expect(app.page).toHaveURL(/\/admin\/merchants(\?|$)/)
    await expect(app.page).not.toHaveURL(new RegExp(`/admin/merchants/${merchantId}(?:\\?|$)`))
  })

  test('PW-M360-API-020 panel GET detail merchantId matches created row', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Api360 ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!
    const listed = await client.getMerchant(merchantId)
    expectStatus(listed, 200)
    expect(listed.body.merchantId).toBe(merchantId)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)
    const detail = waitForBffResponse(page, { method: 'GET', pathExact: `/api/merchants/${merchantId}` })
    await app.merchants.open360(reference)
    const response = await detail
    expect(response.status()).toBe(200)
    const body = await response.json()
    expect(body).toEqual(expect.objectContaining({ merchantId }))
    await expect(app.merchantSlideover.dialog()).toBeVisible()
  })

  test('PW-M360-E2E-061 dialog ARIA snapshot has heading and close', async ({ app, api }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.createMerchant(reference, `Aria ${reference}`)).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()
    await expect(app.merchantSlideover.dialog()).toMatchAriaSnapshot(`
      - heading "Merchant 360"
      - button "Close"
    `)
  })

  test('PW-M360-E2E-062 Escape hides dialog and returns focus to the row', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.createMerchant(reference, `Esc ${reference}`)).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)
    const trigger = page.getByRole('button', { name: `Open ${reference}` })
    await trigger.click()
    await app.merchantSlideover.expectOpen()
    await app.merchantSlideover.closeWithEscape()
    await app.merchantSlideover.expectClosed()
    await expect(trigger).toBeFocused()
    await expect(page).toHaveURL(/\/admin\/merchants/)
  })

  test('PW-M360-E2E-063 Confirm suspend dismiss does not POST', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Susp ${reference}`)
    expectStatus(created, 201)
    expect((await client.activateMerchant(created.body.merchantId!, etagOf(created.headers)!)).status).toBe(200)
    const suspendPath = `/api/merchants/${created.body.merchantId}/suspend`

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()

    let suspendPosted = false
    const onRequest = (request: { method: () => string, url: () => string }) => {
      try {
        if (request.method() === 'POST' && new URL(request.url()).pathname === suspendPath) {
          suspendPosted = true
        }
      }
      catch {
        // ignore malformed URLs
      }
    }
    page.on('request', onRequest)
    try {
      await app.merchantSlideover.openSuspendConfirm()
      await app.merchantSlideover.confirm.dismiss()
      await expect(page.getByRole('heading', { name: 'Suspend merchant' })).toHaveCount(0)
      await expect(app.merchantSlideover.dialog()).toBeVisible()
      expect(suspendPosted).toBe(false)
    }
    finally {
      page.off('request', onRequest)
    }
    expect(merchant360Journey.nestedSuspendDismiss.confirmDismissMustNotPost).toBe('/suspend')
  })

  test('PW-M360-E2E-132 activate then 360 timeline contains ACTIVE', async ({ app, api }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Tl ${reference}`)
    expectStatus(created, 201)
    expect((await client.activateMerchant(created.body.merchantId!, etagOf(created.headers)!)).status).toBe(200)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()
    await expect(app.merchantSlideover.timeline()).toBeVisible()
    await expect(app.merchantSlideover.timeline()).toContainText('ACTIVE')
  })

  test('PW-M360-E2E-133-360 timeline includes payment history transitions', async ({
    app,
    playwright,
  }, testInfo) => {
    const index = testInfo.parallelIndex % POM_WORKER_COUNT
    const world = workerMerchant(index)
    const managerApi = await BffClient.create(playwright, workerManagerAuthFile(index))
    try {
      const reference = uniqueOrderReference(testInfo, '360H')
      const created = await managerApi.createPaymentOrder(
        world.merchantId,
        { amountMinor: 1800, currency: 'PLN', clientOrderReference: reference },
        uniqueIdempotencyKey(testInfo, '360HC'),
      )
      expectStatus(created, 201)
      expect((await managerApi.authorizePayment(
        world.merchantId,
        created.body.paymentOrderId!,
        etagOf(created.headers),
        uniqueIdempotencyKey(testInfo, '360HA'),
      )).status).toBe(200)

      await app.merchants.goto()
      await app.merchants.expectLoaded()
      await app.merchants.filterByText(world.merchantReference)
      await app.merchants.applyFilters()
      await app.merchants.expectRowVisible(world.merchantReference)
      await app.merchants.open360(world.merchantReference)
      await app.merchantSlideover.expectOpen()
      await expect(app.merchantSlideover.dialog().getByRole('heading', { name: 'Notes' })).toBeVisible()
      await expect(app.merchantSlideover.timeline()).toContainText('AUTHORIZED')
      await expect(app.merchantSlideover.timeline()).toContainText(reference)
    } finally {
      await managerApi.dispose()
    }
  })

  test('PW-M360-E2E-064 registry current nav is Registry not Payment Orders', async ({ app }) => {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await expect(app.page.getByTestId('nav-link-merchants')).toHaveAttribute('aria-current', 'page')
    const paymentsNav = app.sidebar.paymentOrders()
    await expect(paymentsNav).toBeVisible()
    await expect(paymentsNav).not.toHaveAttribute('aria-current', 'page')
    await expect(paymentsNav).not.toHaveAttribute('href', '/admin/merchants')
  })

  test('PW-M360-E2E-065 360 View payment orders opens the merchant list', async ({ app, api }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `PayNav ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(reference)
    await app.merchants.applyFilters()
    await app.merchants.expectRowVisible(reference)
    await app.merchants.open360(reference)
    await app.merchantSlideover.expectOpen()
    await app.merchantSlideover.openPaymentOrders()
    await expect(app.page).toHaveURL(new RegExp(`/admin/merchants/${merchantId}/payments`))
    await expect(app.page.getByRole('heading', { name: 'Payment Orders', exact: true })).toBeVisible()
    await expect(app.sidebar.paymentOrders()).toHaveAttribute('aria-current', 'page')
    await expect(app.page.getByTestId('nav-link-merchants')).not.toHaveAttribute('aria-current', 'page')
  })
})
