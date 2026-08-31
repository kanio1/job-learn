import { uniqueMerchantReference, uniqueToken } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { waitForBffResponse } from '../utils/wait-bff'
import { etagOf } from '../utils/http'

function merchantsListQuery(responseUrl: string): URLSearchParams {
  return new URL(responseUrl).searchParams
}

test('PW-M360-E2E-020 click Updated columnheader sends sort=updatedAt', async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  const sorted = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.sortBy(/Updated/)
  const response = await sorted
  expect(response.status()).toBe(200)
  expect(merchantsListQuery(response.url()).get('sort') ?? '').toMatch(/updatedAt/)
})

test('PW-M360-E2E-021 search unique ref Apply shows row and GET q=', async ({ app, api, page }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Search ${reference}`)
  expectStatus(created, 201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.applyFilters()
  const response = await listed
  expect(response.status()).toBe(200)
  expect(merchantsListQuery(response.url()).get('q')).toBe(reference)
  await expect(app.merchants.rowCell(reference)).toBeVisible()
})

test('PW-M360-E2E-022 status ACTIVE plus q returns only ACTIVE rows', async ({ app, api, page }, testInfo) => {
  const client = api
  const token = uniqueToken().slice(0, 10)
  const draftRef = `POM-${testInfo.workerIndex}-${token}D`.slice(0, 32)
  const activeRef = `POM-${testInfo.workerIndex}-${token}A`.slice(0, 32)
  const draft = await client.merchants.create(draftRef, `Draft ${token}`)
  const active = await client.merchants.create(activeRef, `Active ${token}`)
  expectStatus(draft, 201)
  expectStatus(active, 201)
  expect((await client.merchants.activate(active.body.merchantId!, etagOf(active.headers)!)).status).toBe(200)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(token)
  // Network oracle: select the status option without the POM's internal wait
  // so the spec captures the exact GET /api/merchants response and its query.
  await app.merchants.statusFilter().click()
  await app.merchants.filterOption('Active').click()
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.applyFilters()
  const response = await listed
  expect(response.status()).toBe(200)
  const query = merchantsListQuery(response.url())
  expect(query.get('q')).toBe(token)
  expect(query.get('status')).toBe('ACTIVE')
  await expect(app.merchants.rowCell(activeRef)).toBeVisible()
  await expect(app.merchants.tableText(draftRef)).toHaveCount(0)
  const rows = app.merchants.tokenRows(token)
  await expect(rows).toHaveCount(1)
  // Count above proves a single row, so this does not suppress locator ambiguity.
  await expect(rows.first()).toContainText('ACTIVE')
})

test('PW-M360-E2E-023 two Updated clicks send opposite updatedAt directions', async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  const first = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.sortBy(/Updated/)
  const firstSort = merchantsListQuery((await first).url()).get('sort') ?? ''
  const second = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.sortBy(/Updated/)
  const secondSort = merchantsListQuery((await second).url()).get('sort') ?? ''
  expect(firstSort).toMatch(/^updatedAt,(asc|desc)$/)
  expect(secondSort).toMatch(/^updatedAt,(asc|desc)$/)
  expect(secondSort).not.toBe(firstSort)
})

test('PW-M360-E2E-024 displayName sort is monotonic for three owned merchants', async ({ app, api, page }, testInfo) => {
  const client = api
  const token = uniqueToken().slice(0, 10)
  const names = [`Aaa ${token}`, `Mmm ${token}`, `Zzz ${token}`]
  const references = names.map((_, index) => `POM-${testInfo.workerIndex}-${token}${index}`.slice(0, 32))
  for (const [index, displayName] of names.entries()) {
    expect((await client.merchants.create(references[index]!, displayName)).status).toBe(201)
  }

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(token)
  await app.merchants.applyFilters()
  await app.merchants.expectLoaded()
  const sorted = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.sortBy(/Display Name/)
  const response = await sorted
  expect(response.status()).toBe(200)
  expect(merchantsListQuery(response.url()).get('sort') ?? '').toMatch(/displayName/)
  for (const [index, name] of names.entries()) {
    await expect(app.merchants.openButton(references[index]!)).toHaveText(name)
  }
  const tops = await Promise.all(
    references.map(reference => app.merchants.openButton(reference).evaluate(el => el.getBoundingClientRect().top)),
  )
  const ordered = names
    .map((name, index) => ({ name, top: tops[index]! }))
    .sort((a, b) => a.top - b.top)
    .map(row => row.name)
  const asc = [...names]
  const desc = [...names].reverse()
  expect(ordered).toEqual(ordered[0] === asc[0] ? asc : desc)
})

test('PW-M360-E2E-030 Apply from ?page=1 resets to page 0', async ({ app, api, page }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  expect((await client.merchants.create(reference, `Page ${reference}`)).status).toBe(201)

  await app.merchants.goto('?page=1')
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.applyFilters()
  const seen = await listed
  const pageParam = merchantsListQuery(seen.url()).get('page')
  expect(pageParam === null || pageParam === '0').toBe(true)
  await expect(app.page).not.toHaveURL(/page=1/)
  await expect(app.merchants.rowCell(reference)).toBeVisible()
})

test('PW-M360-E2E-031 Back from detail keeps filter URL and row', async ({ app, api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Back ${reference}`
  const created = await client.merchants.create(reference, displayName)
  expectStatus(created, 201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await app.merchants.expectLoaded()
  await expect(app.page).toHaveURL(new RegExp(`q=${reference}`))
  await app.merchantDetail.gotoMerchant(created.body.merchantId!)
  await app.merchantDetail.expectLoaded()
  await app.page.goBack()
  await app.merchants.expectLoaded()
  await expect(app.page).toHaveURL(new RegExp(`q=${reference}`))
  await expect(app.merchants.rowCell(reference)).toBeVisible()
})

test('PW-M360-E2E-032 platform tenant filter sends tenantId and hides other tenant', async ({ app, api, page }, testInfo) => {
  const client = api
  const alphaRef = uniqueMerchantReference(testInfo)
  const platformRef = `P-${uniqueMerchantReference(testInfo)}`.slice(0, 32)
  expect((await client.merchants.create(alphaRef, `Alpha ${alphaRef}`, 'TENANT_ALPHA')).status).toBe(201)
  expect((await client.merchants.create(platformRef, `Plat ${platformRef}`, 'PLATFORM_TENANT')).status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.filterByTenant('TENANT_ALPHA')
  const response = await listed
  expect(response.status()).toBe(200)
  expect(merchantsListQuery(response.url()).get('tenantId')).toBe('TENANT_ALPHA')
  await app.merchants.filterByText(alphaRef)
  await app.merchants.applyFilters()
  await expect(app.merchants.rowCell(alphaRef)).toBeVisible()

  await app.merchants.filterByText(platformRef)
  await app.merchants.applyFilters()
  await expect(app.merchants.filteredEmpty()).toBeVisible()
  await expect(app.merchants.registryTable()).toHaveCount(0)
})

test('PW-M360-API-010 BffClient totalElements matches registry caption', async ({ app, api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  expect((await client.merchants.create(reference, `Caption ${reference}`)).status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await app.merchants.expectLoaded()
  const listed = await client.merchants.list({ q: reference })
  expectStatus(listed, 200)
  expect(listed.body?.totalElements).toBe(1)
  await expect(app.merchants.caption()).toHaveText('1 merchant(s)')
})

test('PW-M360-E2E-040 bulk activate two DRAFT merchants posts twice', async ({ app, api, page }, testInfo) => {
  const client = api
  const token = uniqueToken().slice(0, 10)
  const firstRef = `POM-${testInfo.workerIndex}-${token}1`.slice(0, 32)
  const secondRef = `POM-${testInfo.workerIndex}-${token}2`.slice(0, 32)
  let firstId = ''
  let secondId = ''
  await test.step('arrange two DRAFT merchants via the BFF', async () => {
    const first = await client.merchants.create(firstRef, `Bulk ${firstRef}`)
    const second = await client.merchants.create(secondRef, `Bulk ${secondRef}`)
    expectStatus(first, 201)
    expectStatus(second, 201)
    firstId = first.body.merchantId!
    secondId = second.body.merchantId!
  })

  let firstPost: ReturnType<typeof waitForBffResponse>
  let secondPost: ReturnType<typeof waitForBffResponse>
  await test.step('select both rows and submit bulk activation', async () => {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.filterByText(token)
    await app.merchants.applyFilters()
    await app.merchants.selectRow(firstRef)
    await app.merchants.selectRow(secondRef)
    firstPost = waitForBffResponse(page, { method: 'POST', pathExact: `/api/merchants/${firstId}/activate` })
    secondPost = waitForBffResponse(page, { method: 'POST', pathExact: `/api/merchants/${secondId}/activate` })
    await app.merchants.bulkActivate()
  })
  await test.step('observe one successful activation per selected row', async () => {
    expect((await firstPost).status()).toBe(200)
    expect((await secondPost).status()).toBe(200)
    await expect(app.merchants.rowByReference(firstRef)).toContainText('ACTIVE')
    await expect(app.merchants.rowByReference(secondRef)).toContainText('ACTIVE')
  })
})

test('PW-M360-E2E-041 mixed DRAFT and ACTIVE bulk activate continues after 409', async ({ app, api, page }, testInfo) => {
  const client = api
  const token = uniqueToken().slice(0, 10)
  const draftRef = `POM-${testInfo.workerIndex}-${token}D`.slice(0, 32)
  const activeRef = `POM-${testInfo.workerIndex}-${token}A`.slice(0, 32)
  const draft = await client.merchants.create(draftRef, `Mix ${draftRef}`)
  const active = await client.merchants.create(activeRef, `Mix ${activeRef}`)
  expectStatus(draft, 201)
  expectStatus(active, 201)
  expect((await client.merchants.activate(active.body.merchantId!, etagOf(active.headers)!)).status).toBe(200)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(token)
  await app.merchants.applyFilters()
  await app.merchants.selectRow(draftRef)
  await app.merchants.selectRow(activeRef)
  const draftPost = waitForBffResponse(page, { method: 'POST', pathExact: `/api/merchants/${draft.body.merchantId}/activate` })
  const activePost = waitForBffResponse(page, { method: 'POST', pathExact: `/api/merchants/${active.body.merchantId}/activate` })
  await app.merchants.bulkActivate()
  expect((await draftPost).status()).toBe(200)
  expect((await activePost).status()).toBe(409)
  await expect(app.merchants.activationFailure()).toBeVisible()
  await expect(app.merchants.rowByReference(draftRef)).toContainText('ACTIVE')
  await expect(app.merchants.rowByReference(activeRef)).toContainText('ACTIVE')
})

test('PW-M360-E2E-042 readonly has no selection checkboxes or bulk activate', async ({ actors }) => {
  const { app } = await actors.open('readOnlyUser')
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await expect(app.merchants.selectionCheckboxes()).toHaveCount(0)
    await expect(app.merchants.bulkActivateButton()).toHaveCount(0)
})

test('PW-M360-E2E-050 unique q with no hits shows filtered empty state', async ({ app }, testInfo) => {
  const reference = uniqueMerchantReference(testInfo)
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await expect(app.merchants.filteredEmpty()).toBeVisible()
  await expect(app.merchants.registryTable()).toHaveCount(0)
})

test('PW-M360-E2E-051 applying filters requests the live merchant listing and settles loading state', async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText('POM-LOADING-STATUS')
  const listed = await Promise.all([
    waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' }),
    app.merchants.applyButton().click(),
  ]).then(([response]) => response)
  expect(listed.status()).toBe(200)
  // A real local response can finish before Playwright observes a rendered frame.
  // The suite forbids response interception, so assert the stable post-condition.
  await expect(app.merchants.loadingStatus()).toHaveCount(0)
})

test('PW-M360-E2E-052 merchant manager registry is 403 UI', async ({ actors }) => {
  const { app } = await actors.open('merchantManager')
    await app.merchants.goto()
    await app.merchants.expectAccessDenied()
})

test('PW-M360-API-060 PATCH displayName 200 via BFF', async ({ api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Orig ${reference}`)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId!
  const nextName = `Renamed ${uniqueToken()}`
  const patched = await client.merchants.patchDisplayName(
    merchantId,
    nextName,
    etagOf(created.headers)!,
  )
  expectStatus(patched, 200)
  expect(patched.body.displayName).toBe(nextName)
  expect(patched.body.merchantReference).toBe(reference)
  const got = await client.merchants.get(merchantId)
  expectStatus(got, 200)
  expect(got.body.displayName).toBe(nextName)
})

test('PW-M360-E2E-150 inline save 200 updates the row name', async ({ app, api, page }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `Before ${reference}`)
  expectStatus(created, 201)
  const nextName = `After ${uniqueToken()}`

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await expect(app.merchants.rowCell(reference)).toBeVisible()
  await app.merchants.editDisplayName(reference)
  await app.merchants.fillDisplayName(nextName)
  const patched = waitForBffResponse(page, {
    method: 'PATCH',
    pathExact: `/api/merchants/${created.body.merchantId}`,
  })
  await app.merchants.saveDisplayName(reference)
  expect((await patched).status()).toBe(200)
  await expect(app.merchants.rowByReference(reference)).toContainText(nextName)
})

test('PW-M360-E2E-151 stale If-Match 412 keeps the old name', async ({ app, api, page }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const originalName = `Keep ${reference}`
  const created = await client.merchants.create(reference, originalName)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId!

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await expect(app.merchants.rowCell(reference)).toBeVisible()
  await app.merchants.editDisplayName(reference)
  await app.merchants.fillDisplayName(`Lost ${uniqueToken()}`)
  expect((await client.merchants.activate(merchantId, etagOf(created.headers)!)).status).toBe(200)
  const patched = waitForBffResponse(page, {
    method: 'PATCH',
    pathExact: `/api/merchants/${merchantId}`,
  })
  await app.merchants.saveDisplayName(reference)
  expect((await patched).status()).toBe(412)
  const got = await client.merchants.get(merchantId)
  expectStatus(got, 200)
  expect(got.body.displayName).toBe(originalName)
  await expect(app.merchants.errorState()).toBeVisible()
  await expect(app.merchants.nameInput()).toBeVisible()
})

test('PW-M360-E2E-066 Payments row action opens the merchant payment list', async ({ app, api }, testInfo) => {
  const client = api
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.merchants.create(reference, `RowPay ${reference}`)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId!

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await expect(app.merchants.rowCell(reference)).toBeVisible()
  await app.merchants.openPayments(reference)
  await expect(app.page).toHaveURL(new RegExp(`/admin/merchants/${merchantId}/payments`))
  await expect(app.payments.heading()).toBeVisible()
})
