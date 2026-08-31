import { merchantAlphaId } from '../auth/accounts'
import { test, expect } from '../fixtures'
import { expectProblem } from '../utils/http'
import { merchantColumnAccessMatrix } from '../methods/decision-table/MerchantColumnAccessMatrix'

test.describe('Merchant registry RBAC columns', { tag: ['@security'] }, () => {
  test('PW-M360-SEC-010 readonly has no mutate controls', async ({ actors }) => {
    expect(merchantColumnAccessMatrix.find(row => row.testId === 'PW-M360-SEC-010')?.create).toBe(false)
    const { app } = await actors.open('readOnlyUser')
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(app.merchants.mutationButtons()).toHaveCount(0)
    await expect(app.merchants.createButton()).toHaveCount(0)
    await expect(app.merchants.bulkActivateButton()).toHaveCount(0)
    await expect(app.merchants.importButton()).toHaveCount(0)
    await expect(app.merchants.riskToggle()).toHaveCount(0)
    await expect(app.merchants.tenantColumn()).toHaveCount(0)
  })

  test('PW-M360-API-040 readonly POST activate is 403 problem', async ({ actors }) => {
    const { api } = await actors.open('readOnlyUser')
    const activate = await api.merchants.activate(merchantAlphaId)
    expect(activate.status).toBe(403)
    expectProblem(activate.body, 403)
  })

  test('PW-M360-SEC-011 support sees table without create or activate', async ({ actors }) => {
    const { app } = await actors.open('supportAgent')
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(app.merchants.createButton()).toHaveCount(0)
    await expect(app.merchants.activateButtons()).toHaveCount(0)
    await expect(app.merchants.importButton()).toHaveCount(0)
    await expect(app.sidebar.audit()).toBeVisible()
  })

  test('PW-M360-SEC-012 platform admin sees Create and Tenant column', async ({ actors }) => {
    const { app } = await actors.open('platformAdmin')
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(app.merchants.createButton()).toBeVisible()
    await expect(app.merchants.tenantColumn()).toBeVisible()
  })

  test('PW-M360-SEC-013 tenant admin has no Tenant column', async ({ actors }) => {
    const { app } = await actors.open('tenantAdmin')
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(app.merchants.createButton()).toBeVisible()
    await expect(app.merchants.tenantColumn()).toHaveCount(0)
  })

  test('PW-M360-SEC-014 merchant manager registry is 403 UI', async ({ actors }) => {
    const { app } = await actors.open('merchantManager')
    await app.merchants.goto()
    await app.merchants.expectAccessDenied()
  })
})
