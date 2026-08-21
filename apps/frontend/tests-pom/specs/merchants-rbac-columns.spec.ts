import { merchantAlphaId } from '../auth/accounts'
import { BffClient } from '../api/bff-client'
import { pomAuthFiles } from '../utils/env'
import { test, expect } from '../fixtures'
import { App } from '../pages/App'
import { expectProblem } from '../utils/http'
import { merchantColumnAccessMatrix } from '../methods/decision-table/MerchantColumnAccessMatrix'

test.describe('Merchant registry RBAC columns', { tag: ['@security'] }, () => {
  test('PW-M360-SEC-010 readonly has no mutate controls', async ({ browser }) => {
    expect(merchantColumnAccessMatrix.find(row => row.testId === 'PW-M360-SEC-010')?.create).toBe(false)
    const context = await browser.newContext({ storageState: pomAuthFiles.readOnlyUser })
    const page = await context.newPage()
    const app = new App(page)
    try {
      await app.merchants.goto()
      await app.merchants.expectRegistryTable()
      await expect(page.getByRole('button', { name: /Activate|Suspend|Create merchant/ })).toHaveCount(0)
      await expect(page.getByTestId('action-create-merchant')).toHaveCount(0)
      await expect(page.getByTestId('merchant-bulk-activate')).toHaveCount(0)
      await expect(page.getByRole('button', { name: /Import/ })).toHaveCount(0)
      await expect(page.getByTestId('merchant-risk-toggle')).toHaveCount(0)
      await expect(page.getByRole('columnheader', { name: 'Tenant' })).toHaveCount(0)
    }
    finally {
      await context.close()
    }
  })

  test('PW-M360-API-040 readonly POST activate is 403 problem', async ({ playwright }) => {
    const readonlyApi = await BffClient.create(playwright, pomAuthFiles.readOnlyUser)
    try {
      const activate = await readonlyApi.activateMerchant(merchantAlphaId)
      expect(activate.status).toBe(403)
      expectProblem(activate.body, 403)
    }
    finally {
      await readonlyApi.dispose()
    }
  })

  test('PW-M360-SEC-011 support sees table without create or activate', async ({ browser }) => {
    const context = await browser.newContext({ storageState: pomAuthFiles.supportAgent })
    const page = await context.newPage()
    const app = new App(page)
    try {
      await app.merchants.goto()
      await app.merchants.expectRegistryTable()
      await expect(page.getByTestId('action-create-merchant')).toHaveCount(0)
      await expect(page.getByRole('button', { name: /^Activate / })).toHaveCount(0)
      await expect(page.getByRole('button', { name: /Import/ })).toHaveCount(0)
      await app.sidebar.expectAuditVisible(true)
    }
    finally {
      await context.close()
    }
  })

  test('PW-M360-SEC-012 platform admin sees Create and Tenant column', async ({ browser }) => {
    const context = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
    const page = await context.newPage()
    const app = new App(page)
    try {
      await app.merchants.goto()
      await app.merchants.expectRegistryTable()
      await expect(page.getByTestId('action-create-merchant')).toBeVisible()
      await expect(page.getByRole('columnheader', { name: 'Tenant' })).toBeVisible()
    }
    finally {
      await context.close()
    }
  })

  test('PW-M360-SEC-013 tenant admin has no Tenant column', async ({ browser }) => {
    const context = await browser.newContext({ storageState: pomAuthFiles.tenantAdmin })
    const page = await context.newPage()
    const app = new App(page)
    try {
      await app.merchants.goto()
      await app.merchants.expectRegistryTable()
      await expect(page.getByTestId('action-create-merchant')).toBeVisible()
      await expect(page.getByRole('columnheader', { name: 'Tenant' })).toHaveCount(0)
    }
    finally {
      await context.close()
    }
  })

  test('PW-M360-SEC-014 merchant manager registry is 403 UI', async ({ browser }) => {
    const context = await browser.newContext({ storageState: pomAuthFiles.merchantManager })
    const page = await context.newPage()
    const app = new App(page)
    try {
      await app.merchants.goto()
      await app.merchants.expectAccessDenied()
    }
    finally {
      await context.close()
    }
  })
})
