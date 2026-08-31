import { uniqueMerchantReference } from '../data/factories'
import { merchantAlphaId } from '../auth/accounts'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { etagOf, expectMerchantError, expectProblem } from '../utils/http'
import { merchantIllegalReactivate } from '../methods/state/MerchantStatusMachine'
import { assertPersistedMerchant } from '../utils/persistence'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { waitForBffResponse } from '../utils/wait-bff'
import { observeRequests } from '../utils/network-observer'
import {
  merchantReferenceForLength,
  merchantReferenceLengthPartitions,
  merchantReferencePartitions,
} from '../methods/ep-bva/MerchantReferencePartitions'
import { createMerchantJourney } from '../methods/use-case/CreateMerchantJourney'
import { merchantCreateFormMatrix } from '../methods/decision-table/MerchantCreateFormMatrix'
import { z } from 'zod'

const merchantListSchema = z.object({
  content: z.array(z.unknown()).optional(),
  totalElements: z.number().optional(),
}).passthrough()
const paymentSummarySchema = z.object({ totalOrders: z.number().optional() }).passthrough()
const expirationSweepSchema = z.object({ expiredCount: z.number().optional() }).passthrough()

test('PW-M360-E2E-001 overview Summary merchants equals GET list totalElements', async ({ app, page }) => {
  await test.step('load the overview and capture the Summary and merchant-list responses', async () => {
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
    const body = merchantListSchema.parse(await listedResponse.json())
    expect(body.totalElements).toEqual(expect.any(Number))
    expect(body.content).toEqual(expect.any(Array))
    const pageLength = body.content?.length ?? 0
    await test.step('assert the Summary card equals the API totals', async () => {
      const summarySection = app.overview.summary()
      await expect(app.overview.summaryHeading()).toBeVisible()
      await expect(summarySection.getByText('Merchants')).toBeVisible()
      await expect(summarySection.getByText(String(body.totalElements), { exact: true })).toBeVisible()
      if (pageLength !== body.totalElements) {
        expect(body.totalElements).not.toBe(pageLength)
      }
      const summaryResponse = await summaryWait
      expect(summaryResponse.status()).toBe(200)
    const summaryBody = paymentSummarySchema.parse(await summaryResponse.json())
      if (summaryBody.totalOrders !== undefined) {
        await expect(summarySection.getByText('Payment Orders')).toBeVisible()
        await expect(summarySection.getByText(String(summaryBody.totalOrders), { exact: true })).toBeVisible()
      }
    })
  })
  await expect(app.sidebar.overview()).toBeVisible()
  await expect(app.sidebar.users()).toBeVisible()
  await expect(app.problem.root()).toHaveCount(0)
  await expect(app.overview.merchantForbiddenHint()).toHaveCount(0)
})

test('creates a unique merchant that appears in the registry', async ({ app, api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `POM Merchant ${reference}`
  const created = await client.merchants.create(reference, displayName)
  expectStatus(created, 201)
  expect(created.body.merchantId).toBeTruthy()

  await app.merchantDetail.gotoMerchant(created.body.merchantId!)
  await app.merchantDetail.expectLoaded()
  await expect(app.merchantDetail.reference()).toHaveText(reference)
  await expectNoTokenInBrowserStorage(app.page)
})

test('duplicate merchant reference is 409 from the BFF', async ({ api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Dup ${reference}`)
  expectStatus(created, 201)

  const again = await client.merchants.create(reference, `Dup again ${reference}`)
  expect(again.status).toBe(merchantReferencePartitions.duplicate.expectStatus)
  expectProblem(again.body, 409, merchantReferencePartitions.duplicate.error)
})

test('reactivating a SUSPENDED merchant is 409 (SCN-MER-04)', async ({ api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Reactivate ${reference}`)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId!
  const afterActivate = await client.merchants.activate(merchantId, etagOf(created.headers)!)
  expectStatus(afterActivate, 200)
  const afterSuspend = await client.merchants.suspend(merchantId, etagOf(afterActivate.headers)!)
  expectStatus(afterSuspend, 200)
  const again = await client.merchants.activate(merchantId, etagOf(afterSuspend.headers)!)
  expect(again.status).toBe(merchantIllegalReactivate.expectStatus)
  expectMerchantError(again.body, merchantIllegalReactivate.error)
})

test('activates a DRAFT merchant then suspends it', async ({ app, api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Lifecycle ${reference}`)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId
  expect(merchantId).toBeTruthy()

  await app.merchantDetail.gotoMerchant(merchantId!)
  await app.merchantDetail.expectLoaded()
  await expect(app.merchantDetail.statusBadge()).toContainText('Draft')

  await app.merchantDetail.activate()
  await expect(app.merchantDetail.statusBadge()).toContainText('Active')

  await app.merchantDetail.suspend()
  await expect(app.merchantDetail.statusBadge()).toContainText('Suspended')
})

test('UI create persist: unique merchant GET after API create and reload', async ({ api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Persist ${reference}`
  const created = await client.merchants.create(reference, displayName)
  expectStatus(created, 201)
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
  await expect(app.merchants.tenantReferenceInput()).toBeVisible()
  await app.merchants.fillCreateForm(reference, `Platform create ${reference}`, 'TENANT_ALPHA')
  const created = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants' })
  await app.merchants.submitCreate()
  expect((await created).status()).toBe(201)
  await expect(app.merchants.rowCell(reference)).toBeVisible()
})

test('merchant reference length BVA stays on BFF REST (SCN-MER-07/08/10/11)', async ({ api }, testInfo) => {
  const client = api
  for (const row of merchantReferenceLengthPartitions) {
    await test.step(row.id, async () => {
      let created
      for (let attempt = 0; attempt < 5; attempt++) {
        const reference = merchantReferenceForLength(
          row.length,
          `${uniqueMerchantReference(testInfo)}${row.id}${attempt}`,
        )
        expect(reference.length, row.id).toBe(row.length)
        created = await client.merchants.create(reference, `Len ${row.id}`)
        if (row.expectStatus === 201 && created.status === 409) {
          continue
        }
        break
      }
      if (!created) {
        throw new Error('createMerchant attempt loop must assign a result')
      }
      if (row.expectStatus === 400) {
        expect(created.status, row.id).toBe(400)
        expectMerchantError(created.body, 'validation')
        return
      }
      expectStatus(created, 201, row.id)
      expect(created.body.merchantId, row.id).toBeTruthy()
    })
  }
})

test('PW-M360-E2E-071 duplicate merchant reference shows 409 on the create form', async ({ app, api, page }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Dup UI ${reference}`)
  expectStatus(created, 201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await app.merchants.fillCreateForm(reference, `Dup UI again ${reference}`, 'TENANT_ALPHA')
  const posted = page.waitForResponse(response =>
    response.request().method() === 'POST' && new URL(response.url()).pathname === '/api/merchants')
  await app.merchants.submitCreate()
  const response = await posted
  expect(response.status()).toBe(409)
  await expect(app.merchants.createAlert()).toBeVisible()
  await expect(app.merchants.referenceInput()).toHaveValue(reference)
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

      const { requests } = await observeRequests(page, (request) => {
        try {
          return request.method() === 'POST' && new URL(request.url()).pathname === '/api/merchants'
        }
        catch {
          return false
        }
      }, async () => {
        await app.merchants.fillCreateForm(reference, `BVA ${row.id}`, 'TENANT_ALPHA')
        if (row.expectStatus === 400) {
          await app.merchants.submitCreate()
          if (row.length < 3) {
            await expect(app.merchants.createFieldError('Reference must be at least 3 characters')).toBeVisible()
          } else {
            await expect(app.merchants.createFieldError('Reference must be at most 64 characters')).toBeVisible()
          }
          return
        } else {
          const created = waitForBffResponse(page, { method: 'POST', pathExact: '/api/merchants' })
          await app.merchants.submitCreate()
          expect((await created).status(), row.id).toBe(201)
        }
      })
      if (row.expectStatus === 400) {
        expect(requests, row.id).toHaveLength(0)
        expect(dt?.post).toBe(false)
      }
    })
  }
})

test('PW-M360-E2E-073 create merchant form ARIA snapshot has labelled fields', async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await expect(app.merchants.createForm()).toMatchAriaSnapshot(`
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
  await expect(app.merchants.createFieldError('Reference must be at least 3 characters')).toBeVisible()
  await expect(app.merchants.createFieldError('Name must be at least 2 characters')).toBeVisible()
  expect(posted).toBe(false)
})

test('merchant status filter keeps DRAFT rows and hides them on Active', async ({ app, api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Draft filter ${reference}`)
  expectStatus(created, 201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByStatus('Draft')
  await expect(app.merchants.rowCell(reference)).toBeVisible()
  await app.merchants.filterByStatus('Active')
  await expect(app.merchants.tableText(reference)).toHaveCount(0)
})

test('platform admin expiration sweep is 200 and the payments toolbar shows expiredCount', async ({ app, api, page }) => {
  const client = api
  const rest = await client.operations.runExpirationSweep()
  expectStatus(rest, 200)
  expect(rest.body.expiredCount).toEqual(expect.any(Number))

  await app.payments.gotoForMerchant(merchantAlphaId)
  await app.payments.expectLoaded()
  const posted = page.waitForResponse(response =>
    response.request().method() === 'POST' && response.url().includes('/api/payment-ops/expiration-sweep'),
  )
  await app.payments.runExpirationSweep()
  const response = await posted
  expect(response.status()).toBe(200)
  const body = expirationSweepSchema.parse(await response.json())
  await expect(app.payments.expirationSweepComplete()).toBeVisible()
  await expect(app.payments.expirationSweepCount(body.expiredCount)).toBeVisible()
})
