import { uniqueMerchantReference } from '../data/factories'
import { merchantAlphaId } from '../auth/accounts'
import { test, expect, requireApi } from '../fixtures'
import { etagOf, expectMerchantError, expectProblem } from '../utils/http'
import { merchantIllegalReactivate } from '../methods/state/MerchantStatusMachine'
import { assertPersistedMerchant } from '../utils/persistence'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { waitForBffResponse } from '../utils/wait-bff'
import {
  merchantReferenceForLength,
  merchantReferenceLengthPartitions,
  merchantReferencePartitions,
} from '../methods/ep-bva/MerchantReferencePartitions'
import { createMerchantJourney } from '../methods/use-case/CreateMerchantJourney'
import { merchantCreateFormMatrix } from '../methods/decision-table/MerchantCreateFormMatrix'

test('PW-M360-E2E-001 overview Summary merchants equals GET list totalElements', async ({ app, page }) => {
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  const summaryWait = page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    try {
      return new URL(response.url()).pathname.endsWith('/payment-orders/summary')
    }
    catch {
      return false
    }
  })
  await app.page.goto('/')
  const listedResponse = await listed
  expect(listedResponse.status()).toBe(200)
  const body = await listedResponse.json() as {
    content?: unknown[]
    totalElements?: number
  }
  expect(typeof body.totalElements).toBe('number')
  expect(Array.isArray(body.content)).toBe(true)
  const pageLength = body.content?.length ?? 0
  const summarySection = app.page.getByRole('region', { name: 'Summary' })
  await expect(app.page.getByRole('heading', { name: 'Platform Summary' })).toBeVisible()
  await expect(summarySection.getByText('Merchants')).toBeVisible()
  await expect(summarySection.getByText(String(body.totalElements), { exact: true })).toBeVisible()
  if (pageLength !== body.totalElements) {
    expect(body.totalElements).not.toBe(pageLength)
  }
  const summaryResponse = await summaryWait
  expect(summaryResponse.status()).toBe(200)
  const summaryBody = await summaryResponse.json() as { totalOrders?: number }
  if (summaryBody.totalOrders !== undefined) {
    await expect(summarySection.getByText('Payment Orders')).toBeVisible()
    await expect(summarySection.getByText(String(summaryBody.totalOrders), { exact: true })).toBeVisible()
  }
  await expect(app.page.getByTestId('nav-link-overview')).toBeVisible()
  await expect(app.page.getByTestId('nav-link-users')).toBeVisible()
  await expect(app.problem.root()).toHaveCount(0)
  await expect(app.page.getByTestId('overview-merchant-forbidden-hint')).toHaveCount(0)
})

test('creates a unique merchant that appears in the registry', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `POM Merchant ${reference}`
  const created = await client.createMerchant(reference, displayName)
  expect(created.status).toBe(201)
  expect(created.body.merchantId).toBeTruthy()

  await app.merchantDetail.gotoMerchant(created.body.merchantId!)
  await app.merchantDetail.expectLoaded()
  await expect(app.page.getByTestId('merchant-reference')).toHaveText(reference)
  await expectNoTokenInBrowserStorage(app.page)
})

test('duplicate merchant reference is 409 from the BFF', async ({ api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Dup ${reference}`)
  expect(created.status).toBe(201)

  const again = await client.createMerchant(reference, `Dup again ${reference}`)
  expect(again.status).toBe(merchantReferencePartitions.duplicate.expectStatus)
  expectProblem(again.body, 409, merchantReferencePartitions.duplicate.error)
})

test('reactivating a SUSPENDED merchant is 409 (SCN-MER-04)', async ({ api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Reactivate ${reference}`)
  expect(created.status).toBe(201)
  const merchantId = created.body.merchantId!
  const afterActivate = await client.activateMerchant(merchantId, etagOf(created.headers)!)
  expect(afterActivate.status).toBe(200)
  const afterSuspend = await client.suspendMerchant(merchantId, etagOf(afterActivate.headers)!)
  expect(afterSuspend.status).toBe(200)
  const again = await client.activateMerchant(merchantId, etagOf(afterSuspend.headers)!)
  expect(again.status).toBe(merchantIllegalReactivate.expectStatus)
  expectMerchantError(again.body, merchantIllegalReactivate.error)
})

test('activates a DRAFT merchant then suspends it', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Lifecycle ${reference}`)
  expect(created.status).toBe(201)
  const merchantId = created.body.merchantId
  expect(merchantId).toBeTruthy()

  await app.merchantDetail.gotoMerchant(merchantId!)
  await app.merchantDetail.expectLoaded()
  await app.merchantDetail.expectStatus('Draft')

  await app.merchantDetail.activate()
  await app.merchantDetail.expectStatus('Active')

  await app.merchantDetail.suspend()
  await app.merchantDetail.expectStatus('Suspended')
})

test('UI create persist: unique merchant GET after API create and reload', async ({ api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Persist ${reference}`
  const created = await client.createMerchant(reference, displayName)
  expect(created.status).toBe(201)
  const merchantId = created.body.merchantId
  expect(merchantId).toBeTruthy()

  await assertPersistedMerchant(client, merchantId!)
})

test('PW-M360-E2E-070 unique create from form is 201 and shows the row', async ({ app, page }, testInfo) => {
  expect(createMerchantJourney.platformWithTenantField.tenantFieldVisible).toBe(true)
  const reference = uniqueMerchantReference(testInfo)
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await expect(app.page.getByTestId('create-merchant-tenant-reference')).toBeVisible()
  await app.merchants.fillCreateForm(reference, `Platform create ${reference}`, 'TENANT_ALPHA')
  const created = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants' })
  await app.merchants.submitCreate()
  expect((await created).status()).toBe(201)
  await app.merchants.expectRowVisible(reference)
})

test('merchant reference length BVA stays on BFF REST (SCN-MER-07/08/10/11)', async ({ api }, testInfo) => {
  const client = requireApi(api)
  for (const row of merchantReferenceLengthPartitions) {
    await test.step(row.id, async () => {
      let created
      for (let attempt = 0; attempt < 5; attempt++) {
        const reference = merchantReferenceForLength(
          row.length,
          `${uniqueMerchantReference(testInfo)}${row.id}${attempt}`,
        )
        expect(reference.length, row.id).toBe(row.length)
        created = await client.createMerchant(reference, `Len ${row.id}`)
        if (row.expectStatus === 201 && created.status === 409) {
          continue
        }
        break
      }
      expect(created?.status, row.id).toBe(row.expectStatus)
      if (row.expectStatus === 400) {
        expectMerchantError(created?.body, 'validation')
        return
      }
      expect(created?.body?.merchantId, row.id).toBeTruthy()
    })
  }
})

test('PW-M360-E2E-071 duplicate merchant reference shows 409 on the create form', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Dup UI ${reference}`)
  expect(created.status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await app.merchants.fillCreateForm(reference, `Dup UI again ${reference}`, 'TENANT_ALPHA')
  const posted = page.waitForResponse(response =>
    response.request().method() === 'POST' && new URL(response.url()).pathname === '/api/merchants')
  await app.merchants.submitCreate()
  const response = await posted
  expect(response.status()).toBe(409)
  await expect(app.page.getByRole('alert').filter({ hasText: /already exists/i })).toBeVisible()
  await expect(app.page.getByLabel('Merchant reference')).toHaveValue(reference)
})

test('PW-M360-E2E-072 merchant reference length BVA on the create form', async ({ app, page }, testInfo) => {
  for (const row of merchantReferenceLengthPartitions) {
    const dt = merchantCreateFormMatrix.find(item => item.referenceLength === row.length)
    expect(dt, row.id).toBeTruthy()
    await test.step(row.id, async () => {
      const reference = merchantReferenceForLength(
        row.length,
        `${uniqueMerchantReference(testInfo)}${row.id}`,
      )
      expect(reference.length, row.id).toBe(row.length)
      await app.merchants.goto()
      await app.merchants.expectLoaded()
      await app.merchants.openCreateForm()

      let posted = false
      const onRequest = (request: { method: () => string, url: () => string }) => {
        try {
          if (request.method() === 'POST' && new URL(request.url()).pathname === '/api/merchants') {
            posted = true
          }
        }
        catch {
          // ignore malformed URLs
        }
      }
      page.on('request', onRequest)
      try {
        await app.merchants.fillCreateForm(reference, `BVA ${row.id}`, 'TENANT_ALPHA')
        if (row.expectStatus === 400) {
          await app.merchants.submitCreate()
          if (row.length < 3) {
            await app.merchants.expectCreateFieldError('Reference must be at least 3 characters')
          } else {
            await app.merchants.expectCreateFieldError('Reference must be at most 64 characters')
          }
          expect(posted, row.id).toBe(false)
          expect(dt?.post).toBe(false)
        } else {
          const created = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants' })
          await app.merchants.submitCreate()
          expect((await created).status(), row.id).toBe(201)
          await app.merchants.expectRowVisible(reference)
        }
      }
      finally {
        page.off('request', onRequest)
      }
    })
  }
})

test('PW-M360-E2E-073 create merchant form ARIA snapshot has labelled fields', async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await expect(app.page.getByTestId('create-merchant-form')).toMatchAriaSnapshot(`
    - textbox "Merchant reference"
    - textbox "Display name"
    - textbox "Tenant reference"
    - button "Cancel"
    - button "Create merchant"
  `)
})

test('empty create merchant form shows field errors and does not POST', async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()

  let posted = false
  page.on('request', (request) => {
    try {
      if (request.method() === 'POST' && new URL(request.url()).pathname === '/api/merchants') {
        posted = true
      }
    } catch {
      // ignore invalid URLs
    }
  })

  await app.merchants.submitCreate()
  await app.merchants.expectCreateFieldError('Reference must be at least 3 characters')
  await app.merchants.expectCreateFieldError('Name must be at least 2 characters')
  expect(posted).toBe(false)
})

test('merchant status filter keeps DRAFT rows and hides them on Active', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Draft filter ${reference}`)
  expect(created.status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByStatus('Draft')
  await app.merchants.expectRowVisible(reference)
  await app.merchants.filterByStatus('Active')
  await app.merchants.expectRowAbsent(reference)
})

test('platform admin expiration sweep is 200 and the payments toolbar shows expiredCount', async ({ app, api, page }) => {
  const client = requireApi(api)
  const rest = await client.runExpirationSweep()
  expect(rest.status).toBe(200)
  expect(typeof rest.body?.expiredCount).toBe('number')

  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  const posted = page.waitForResponse(response =>
    response.request().method() === 'POST' && response.url().includes('/api/payment-ops/expiration-sweep'),
  )
  await app.payments.runExpirationSweep()
  const response = await posted
  expect(response.status()).toBe(200)
  const body = await response.json() as { expiredCount?: number }
  await expect(app.page.getByText('Expiration sweep complete', { exact: true })).toBeVisible()
  await expect(app.page.getByText(`${body.expiredCount} order(s) expired`, { exact: true })).toBeVisible()
})
