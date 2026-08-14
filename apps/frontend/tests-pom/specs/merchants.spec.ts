import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { assertPersistedMerchant } from '../utils/persistence'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

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
  expect(again.status).toBe(409)
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
