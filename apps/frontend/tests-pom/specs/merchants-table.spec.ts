import { uniqueMerchantReference, uniqueToken } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { waitForBffResponse } from '../utils/wait-bff'
import { App } from '../pages/App'
import { pomAuthFiles } from '../utils/env'
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
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Search ${reference}`)
  expectStatus(created, 201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.applyFilters()
  const response = await listed
  expect(response.status()).toBe(200)
  expect(merchantsListQuery(response.url()).get('q')).toBe(reference)
  await app.merchants.expectRowVisible(reference)
})

test('PW-M360-E2E-022 status ACTIVE plus q returns only ACTIVE rows', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const token = uniqueToken().slice(0, 10)
  const draftRef = `POM-${testInfo.workerIndex}-${token}D`.slice(0, 32)
  const activeRef = `POM-${testInfo.workerIndex}-${token}A`.slice(0, 32)
  const draft = await client.createMerchant(draftRef, `Draft ${token}`)
  const active = await client.createMerchant(activeRef, `Active ${token}`)
  expectStatus(draft, 201)
  expectStatus(active, 201)
  expect((await client.activateMerchant(active.body.merchantId!, etagOf(active.headers)!)).status).toBe(200)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(token)
  // Network oracle: select the status option without the POM's internal wait
  // so the spec captures the exact GET /api/merchants response and its query.
  await app.page.getByLabel('Filter status').click()
  await app.page.getByRole('option', { name: 'Active' }).click()
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.applyFilters()
  const response = await listed
  expect(response.status()).toBe(200)
  const query = merchantsListQuery(response.url())
  expect(query.get('q')).toBe(token)
  expect(query.get('status')).toBe('ACTIVE')
  await app.merchants.expectRowVisible(activeRef)
  await app.merchants.expectRowAbsent(draftRef)
  const rows = app.page.getByRole('table').getByRole('row').filter({ hasText: token })
  await expect(rows).toHaveCount(1)
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
  const client = requireApi(api)
  const token = uniqueToken().slice(0, 10)
  const names = [`Aaa ${token}`, `Mmm ${token}`, `Zzz ${token}`]
  const references = names.map((_, index) => `POM-${testInfo.workerIndex}-${token}${index}`.slice(0, 32))
  for (const [index, displayName] of names.entries()) {
    expect((await client.createMerchant(references[index]!, displayName)).status).toBe(201)
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
    await expect(app.page.getByRole('button', { name: `Open ${references[index]!}` })).toHaveText(name)
  }
  const tops = await Promise.all(
    references.map(reference => app.page.getByRole('button', { name: `Open ${reference}` }).evaluate(el => el.getBoundingClientRect().top)),
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
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  expect((await client.createMerchant(reference, `Page ${reference}`)).status).toBe(201)

  await app.merchants.goto('?page=1')
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.applyFilters()
  const seen = await listed
  const pageParam = merchantsListQuery(seen.url()).get('page')
  expect(pageParam === null || pageParam === '0').toBe(true)
  await expect(app.page).not.toHaveURL(/page=1/)
  await app.merchants.expectRowVisible(reference)
})

test('PW-M360-E2E-031 Back from detail keeps filter URL and row', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const displayName = `Back ${reference}`
  const created = await client.createMerchant(reference, displayName)
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
  await app.merchants.expectRowVisible(reference)
})

test('PW-M360-E2E-032 platform tenant filter sends tenantId and hides other tenant', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const alphaRef = uniqueMerchantReference(testInfo)
  const platformRef = `P-${uniqueMerchantReference(testInfo)}`.slice(0, 32)
  expect((await client.createMerchant(alphaRef, `Alpha ${alphaRef}`, 'TENANT_ALPHA')).status).toBe(201)
  expect((await client.createMerchant(platformRef, `Plat ${platformRef}`, 'PLATFORM_TENANT')).status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  const listed = waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' })
  await app.merchants.filterByTenant('TENANT_ALPHA')
  const response = await listed
  expect(response.status()).toBe(200)
  expect(merchantsListQuery(response.url()).get('tenantId')).toBe('TENANT_ALPHA')
  await app.merchants.filterByText(alphaRef)
  await app.merchants.applyFilters()
  await app.merchants.expectRowVisible(alphaRef)

  await app.merchants.filterByText(platformRef)
  await app.merchants.applyFilters()
  await expect(app.page.getByText('No merchants match the current filters.')).toBeVisible()
  await expect(app.page.getByRole('table', { name: 'Merchant registry' })).toHaveCount(0)
})

test('PW-M360-API-010 BffClient totalElements matches registry caption', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  expect((await client.createMerchant(reference, `Caption ${reference}`)).status).toBe(201)

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await app.merchants.expectLoaded()
  const listed = await client.listMerchants({ q: reference })
  expectStatus(listed, 200)
  expect(listed.body?.totalElements).toBe(1)
  await expect(app.merchants.caption()).toHaveText('1 merchant(s)')
})

test('PW-M360-E2E-040 bulk activate two DRAFT merchants posts twice', async ({ app, api, page }, testInfo) => {
  await test.step('arrange two DRAFT merchants via the BFF', async () => {
    const client = requireApi(api)
    const token = uniqueToken().slice(0, 10)
    const firstRef = `POM-${testInfo.workerIndex}-${token}1`.slice(0, 32)
    const secondRef = `POM-${testInfo.workerIndex}-${token}2`.slice(0, 32)
    const first = await client.createMerchant(firstRef, `Bulk ${firstRef}`)
    const second = await client.createMerchant(secondRef, `Bulk ${secondRef}`)
    expectStatus(first, 201)
    expectStatus(second, 201)
    const firstId = first.body.merchantId!
    const secondId = second.body.merchantId!

    await test.step('select both rows and bulk activate', async () => {
      await app.merchants.goto()
      await app.merchants.expectLoaded()
      await app.merchants.filterByText(token)
      await app.merchants.applyFilters()
      await app.merchants.selectRow(firstRef)
      await app.merchants.selectRow(secondRef)
      const firstPost = waitForBffResponse(page, { method: 'POST', pathExact: `/api/merchants/${firstId}/activate` })
      const secondPost = waitForBffResponse(page, { method: 'POST', pathExact: `/api/merchants/${secondId}/activate` })
      await app.merchants.bulkActivate()
      await test.step('assert one POST per merchant and the ACTIVE rows', async () => {
        expect((await firstPost).status()).toBe(200)
        expect((await secondPost).status()).toBe(200)
        await expect(app.merchants.rowByReference(firstRef)).toContainText('ACTIVE')
        await expect(app.merchants.rowByReference(secondRef)).toContainText('ACTIVE')
      })
    })
  })
})

test('PW-M360-E2E-041 mixed DRAFT and ACTIVE bulk activate continues after 409', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const token = uniqueToken().slice(0, 10)
  const draftRef = `POM-${testInfo.workerIndex}-${token}D`.slice(0, 32)
  const activeRef = `POM-${testInfo.workerIndex}-${token}A`.slice(0, 32)
  const draft = await client.createMerchant(draftRef, `Mix ${draftRef}`)
  const active = await client.createMerchant(activeRef, `Mix ${activeRef}`)
  expectStatus(draft, 201)
  expectStatus(active, 201)
  expect((await client.activateMerchant(active.body.merchantId!, etagOf(active.headers)!)).status).toBe(200)

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
  await expect(app.page.getByText('Activation failed').first()).toBeVisible()
  await expect(app.merchants.rowByReference(draftRef)).toContainText('ACTIVE')
  await expect(app.merchants.rowByReference(activeRef)).toContainText('ACTIVE')
})

test('PW-M360-E2E-042 readonly has no selection checkboxes or bulk activate', async ({ browser }) => {
  const context = await browser.newContext({ storageState: pomAuthFiles.readOnlyUser })
  const page = await context.newPage()
  const app = new App(page)
  try {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await expect(page.getByRole('checkbox', { name: /Select / })).toHaveCount(0)
    await expect(page.getByTestId('merchant-bulk-activate')).toHaveCount(0)
  } finally {
    await context.close()
  }
})

test('PW-M360-E2E-050 unique q with no hits shows filtered empty state', async ({ app }, testInfo) => {
  const reference = uniqueMerchantReference(testInfo)
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await expect(app.page.getByText('No merchants match the current filters.')).toBeVisible()
  await expect(app.page.getByRole('table', { name: 'Merchant registry' })).toHaveCount(0)
})

test('PW-M360-E2E-051 loading status is visible while GET merchants is in flight', async ({ app, page }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText('POM-LOADING-STATUS')
  const loading = page.getByRole('status', { name: 'Loading merchants…' })
  const [listed] = await Promise.all([
    waitForBffResponse(page, { method: 'GET', pathExact: '/api/merchants' }),
    loading.waitFor({ state: 'visible' }),
    page.getByTestId('merchant-filter-apply').click(),
  ])
  expect(listed.status()).toBe(200)
  await expect(loading).toHaveCount(0)
})

test('PW-M360-E2E-052 merchant manager registry is 403 UI', async ({ browser }) => {
  const context = await browser.newContext({ storageState: pomAuthFiles.merchantManager })
  const page = await context.newPage()
  const app = new App(page)
  try {
    await app.merchants.goto()
    await app.merchants.expectAccessDenied()
  } finally {
    await context.close()
  }
})

test('PW-M360-API-060 PATCH displayName 200 via BFF', async ({ api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Orig ${reference}`)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId!
  const nextName = `Renamed ${uniqueToken()}`
  const patched = await client.patchMerchantDisplayName(
    merchantId,
    nextName,
    etagOf(created.headers)!,
  )
  expectStatus(patched, 200)
  expect(patched.body.displayName).toBe(nextName)
  expect(patched.body.merchantReference).toBe(reference)
  const got = await client.getMerchant(merchantId)
  expectStatus(got, 200)
  expect(got.body.displayName).toBe(nextName)
})

test('PW-M360-E2E-150 inline save 200 updates the row name', async ({ app, api, page }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `Before ${reference}`)
  expectStatus(created, 201)
  const nextName = `After ${uniqueToken()}`

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await app.merchants.expectRowVisible(reference)
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
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const originalName = `Keep ${reference}`
  const created = await client.createMerchant(reference, originalName)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId!

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await app.merchants.expectRowVisible(reference)
  await app.merchants.editDisplayName(reference)
  await app.merchants.fillDisplayName(`Lost ${uniqueToken()}`)
  expect((await client.activateMerchant(merchantId, etagOf(created.headers)!)).status).toBe(200)
  const patched = waitForBffResponse(page, {
    method: 'PATCH',
    pathExact: `/api/merchants/${merchantId}`,
  })
  await app.merchants.saveDisplayName(reference)
  expect((await patched).status()).toBe(412)
  const got = await client.getMerchant(merchantId)
  expectStatus(got, 200)
  expect(got.body.displayName).toBe(originalName)
  await expect(app.page.getByTestId('error-state')).toBeVisible()
  await expect(app.page.getByTestId('merchant-name-input')).toBeVisible()
})

test('PW-M360-E2E-066 Payments row action opens the merchant payment list', async ({ app, api }, testInfo) => {
  const client = requireApi(api)
  const reference = uniqueMerchantReference(testInfo)
  const created = await client.createMerchant(reference, `RowPay ${reference}`)
  expectStatus(created, 201)
  const merchantId = created.body.merchantId!

  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.filterByText(reference)
  await app.merchants.applyFilters()
  await app.merchants.expectRowVisible(reference)
  await app.merchants.openPayments(reference)
  await expect(app.page).toHaveURL(new RegExp(`/admin/merchants/${merchantId}/payments`))
  await expect(app.page.getByRole('heading', { name: 'Payment Orders', exact: true })).toBeVisible()
})
