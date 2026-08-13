import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { assertPersistedMerchant } from '../utils/persistence'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

test('creates a unique merchant that appears in the registry', async ({ app }, testInfo) => {
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `POM Merchant ${reference}`

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await app.merchants.fillCreateForm(reference, displayName)
  await app.merchants.submitCreate()

  await expect(app.page.getByText('Merchant created', { exact: true })).toBeVisible()
  await app.merchants.expectRowVisible(reference)
  await expectNoTokenInBrowserStorage(app.page)
})

test('duplicate merchant reference shows 409 feedback in the create form', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Dup ${reference}`)
  expect(created.status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await app.merchants.fillCreateForm(reference, `Dup again ${reference}`)
  await app.merchants.submitCreate()

  await expect(app.page.getByRole('alert')).toContainText(/already exists/i)
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

test('UI create persists through BFF GET and reload', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Persist ${reference}`

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await app.merchants.fillCreateForm(reference, displayName)

  const created = page.waitForResponse(response =>
    response.url().includes('/api/merchants') && response.request().method() === 'POST',
  )
  await app.merchants.submitCreate()
  const response = await created
  expect(response.status()).toBe(201)
  const body = await response.json() as { merchantId?: string }
  expect(body.merchantId).toBeTruthy()

  await assertPersistedMerchant(client, body.merchantId!)
  await expect(app.page.getByText('Merchant created', { exact: true })).toBeVisible()
  await app.page.reload()
  await app.merchants.expectLoaded()
  await app.merchants.expectRowVisible(reference)
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
