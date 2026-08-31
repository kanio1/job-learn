import { uniqueMerchantReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'

function waitForOrgTreeChildren(page: import('@playwright/test').Page) {
  return page.waitForResponse((response) => {
    if (response.request().method() !== 'GET') {
      return false
    }
    try {
      const url = new URL(response.url())
      return url.pathname === '/api/org-tree' && url.searchParams.has('parent')
    }
    catch {
      return false
    }
  })
}

test.describe('Merchant org tree', { tag: ['@a11y'] }, () => {
  test('PW-M360-E2E-100 expand Alpha shows unique child merchant', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.merchants.create(reference, `Tree ${reference}`)).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    const children = waitForOrgTreeChildren(page)
    await app.merchants.expandTreeItem(/Alpha/)
    const response = await children
    expect(response.status()).toBe(200)
    await expect(app.merchants.treeItem(reference)).toBeVisible()
  })

  test('PW-M360-E2E-101 collapse hides child and aria-expanded', async ({ app, api }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.merchants.create(reference, `Collapse ${reference}`)).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.expandTreeItem(/Alpha/)
    await expect(app.merchants.treeItem(reference)).toBeVisible()
    await expect(app.merchants.treeItem(/Alpha/)).toHaveAttribute('aria-expanded', 'true')
    await app.merchants.collapseTreeItem(/Alpha/)
    await expect(app.merchants.treeItem(reference)).toHaveCount(0)
    await expect(app.merchants.treeItem(/Alpha/)).toHaveAttribute('aria-expanded', 'false')
  })

  test('PW-M360-E2E-102 keyboard expand Alpha', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.merchants.create(reference, `Key ${reference}`)).status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    const children = waitForOrgTreeChildren(page)
    await app.merchants.expandTreeItemWithKeyboard(/Alpha/)
    expect((await children).status()).toBe(200)
    await expect(app.merchants.treeItem(reference)).toBeVisible()
  })

  test('PW-M360-E2E-103 tenant.admin does not see PLATFORM_TENANT children', async ({ actors }, testInfo) => {
    const admin = await actors.open('platformAdmin')
    const reference = uniqueMerchantReference(testInfo)
    expect((await admin.api.merchants.create(reference, `Plat ${reference}`, 'PLATFORM_TENANT')).status).toBe(201)
    const { app: tenantApp } = await actors.open('tenantAdmin')
    await tenantApp.merchants.goto()
    await tenantApp.merchants.expectLoaded()
    await expect(tenantApp.merchants.treeItem(/Alpha/)).toBeVisible()
    await expect(tenantApp.merchants.treeItem(/Platform Tenant/)).toHaveCount(0)
    await tenantApp.merchants.expandTreeItem(/Alpha/)
    await expect(tenantApp.merchants.treeItem(reference)).toHaveCount(0)
  })

  test('PW-M360-E2E-104 org tree ARIA snapshot fragment', async ({ app }) => {
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await expect(app.merchants.treeItem('Alpha Tenant')).toMatchAriaSnapshot(`
      - treeitem "Alpha Tenant"
    `)
  })

  test('PW-M360-API-050 lazy children 200 via BFF', async ({ api }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.merchants.create(reference, `ApiTree ${reference}`)).status).toBe(201)

    const roots = await client.merchants.orgTree()
    expectStatus(roots, 200)
    const alpha = roots.body.nodes?.find(node => node.reference === 'TENANT_ALPHA')
    expect(alpha?.id).toBeTruthy()
    expect(alpha?.lazy).toBe(true)

    const children = await client.merchants.orgTree(alpha!.id)
    expectStatus(children, 200)
    expect(children.body.nodes?.some(node => node.reference === reference)).toBe(true)
  })

  test('deep-link ?tree=tenant-alpha expands Alpha', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    expect((await client.merchants.create(reference, `Link ${reference}`)).status).toBe(201)

    const children = waitForOrgTreeChildren(page)
    await app.merchants.goto('?tree=tenant-alpha')
    await app.merchants.expectLoaded()
    expect((await children).status()).toBe(200)
    await expect(app.merchants.treeItem(/Alpha/)).toHaveAttribute('aria-expanded', 'true')
    await expect(app.merchants.treeItem(reference)).toBeVisible()
  })
})
