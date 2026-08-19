import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectMerchantError, expectProblem } from '../utils/http'
import { merchantIllegalReactivate } from '../methods/state/MerchantStatusMachine'
import { assertPersistedMerchant } from '../utils/persistence'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import {
  merchantReferenceForLength,
  merchantReferenceLengthPartitions,
  merchantReferencePartitions,
} from '../methods/ep-bva/MerchantReferencePartitions'
import { createMerchantJourney } from '../methods/use-case/CreateMerchantJourney'

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
  expect((await client.activateMerchant(merchantId)).status).toBe(200)
  expect((await client.suspendMerchant(merchantId)).status).toBe(200)
  const again = await client.activateMerchant(merchantId)
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

test('UI create persist: unique merchant GET after API create and reload', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Persist ${reference}`
  const created = await client.createMerchant(reference, displayName)
  expect(created.status).toBe(201)
  const merchantId = created.body.merchantId
  expect(merchantId).toBeTruthy()

  await assertPersistedMerchant(client, merchantId!)
})

test('platform admin create form requires tenant reference and persists', async ({ app, api }, testInfo) => {
  expect(createMerchantJourney.platformWithTenantField.tenantFieldVisible).toBe(true)
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await expect(app.page.getByTestId('create-merchant-tenant-reference')).toBeVisible()
  await app.merchants.fillCreateForm(reference, `Platform create ${reference}`, 'TENANT_ALPHA')
  await app.merchants.submitCreate()
  await app.merchants.expectRowVisible(reference)
  const listed = await client.listMerchants()
  expect(listed.body.merchants?.some(row => row.merchantReference === reference)).toBe(true)
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
