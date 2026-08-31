import { expect, test } from '../fixtures'
import { merchantAlphaId, merchantAlphaReference, merchantAlphaTwoId, merchantBetaId, merchantBetaReference } from '../auth/accounts'
import { expectSuccess, expectError } from '../api/contracts/http-result'
import { uniqueIdempotencyKey, uniqueMerchantReference, uniqueOrderReference } from '../data/factories'
import { expectProblem } from '../utils/http'
import { isolationDtUcE2eRows } from '../methods/combinations/IsolationDtUc'
import { mrIso } from '../methods/metamorphic/IsolationInclusion'
import { createMerchantJourney } from '../methods/use-case/CreateMerchantJourney'

function paymentOrderDraft(clientOrderReference: string) {
  return { amountMinor: 1000, currency: 'PLN' as const, clientOrderReference }
}

test.describe('tenant scope vs RBAC @security', () => {
  test('platform admin registry shows Alpha and Beta (SCN-ISO-06)', async ({ actors }) => {
    expect(isolationDtUcE2eRows.some(row => row.id === 'SCN-ISO-06')).toBe(true)
    const { app } = await actors.open('platformAdmin')
    await app.merchantDetail.gotoMerchant(merchantAlphaId)
    await app.merchantDetail.expectLoaded()
    await expect(app.merchantDetail.reference()).toHaveText(merchantAlphaReference)
    await app.merchantDetail.gotoMerchant(merchantBetaId)
    await app.merchantDetail.expectLoaded()
    await expect(app.merchantDetail.reference()).toHaveText(merchantBetaReference)
  })

  test('tenant admin registry shows Alpha and hides Beta (SCN-ISO-01)', async ({ actors }) => {
    const { app, api } = await actors.open('tenantAdmin')
    await app.merchantDetail.gotoMerchant(merchantAlphaId)
    await app.merchantDetail.expectLoaded()
    await expect(app.merchantDetail.reference()).toHaveText(merchantAlphaReference)
    await app.merchantDetail.gotoMerchant(merchantBetaId)
    await expect(app.merchantDetail.reference()).toHaveCount(0)
    expect(expectSuccess(await api.merchants.get(merchantAlphaId), 200).body.merchantReference).toBe(merchantAlphaReference)
    expectError(await api.merchants.get(merchantBetaId), 404)
  })

  test('merchant manager registry is RBAC deny not tenant isolation (SCN-ISO-09)', async ({ actors }) => {
    const { app } = await actors.open('merchantManager')
    await app.merchants.goto()
    await app.merchants.expectAccessDenied()
  })

  test('tenant admin GET Beta is masked 404; GET Alpha is 200 (SCN-ISO-02/03)', async ({ actors }) => {
    const { api } = await actors.open('tenantAdmin')
    expect(expectSuccess(await api.merchants.get(merchantAlphaId), 200).body.merchantReference).toBe(merchantAlphaReference)
    const error = expectError(await api.merchants.get(merchantBetaId), 404)
    expectProblem(error.body, 404)
    const detail = `${error.body.detail ?? ''} ${error.body.title ?? ''}`
    expect(detail.includes('MERCHANT_BETA')).toBe(false)
    expect(detail.includes('PLATFORM_TENANT')).toBe(false)
  })

  test('manager GET own order via ALPHA_002 path is masked 404', async ({ actors }, testInfo) => {
    const { api } = await actors.open('merchantManager')
    const created = expectSuccess(await api.payments.createOrder(merchantAlphaId, paymentOrderDraft(uniqueOrderReference(testInfo, 'BOLA-GET')), uniqueIdempotencyKey(testInfo, 'BOLA-GET')), 201)
    expectProblem(expectError(await api.payments.get(merchantAlphaTwoId, created.body.paymentOrderId), 404).body, 404)
  })

  test('manager POST order on ALPHA_002 is 403 BOLA (SCN-ISO-10)', async ({ actors }, testInfo) => {
    const { api } = await actors.open('merchantManager')
    expectError(await api.payments.createOrder(merchantAlphaTwoId, paymentOrderDraft(uniqueOrderReference(testInfo, 'BOLA')), uniqueIdempotencyKey(testInfo, 'BOLA')), 403)
  })

  test('tenant admin UI create uses JWT tenant and has no tenant field', async ({ actors }, testInfo) => {
    expect(createMerchantJourney.tenantAdminJwt.tenantFieldVisible).toBe(false)
    const { app } = await actors.open('tenantAdmin')
    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchants.openCreateForm()
    await expect(app.merchants.tenantReferenceInput()).toHaveCount(0)
    const reference = uniqueMerchantReference(testInfo)
    await app.merchants.fillCreateForm(reference, `Tenant create ${reference}`)
    await app.merchants.submitCreate()
    await expect(app.merchants.rowCell(reference)).toBeVisible()
  })

  test('MR-ISO: tenant.admin merchants ⊆ platform.admin; Beta excluded', async ({ actors }) => {
    const tenant = await actors.open('tenantAdmin')
    const platform = await actors.open('platformAdmin')
    const tenantList = expectSuccess(await tenant.api.merchants.list(), 200)
    expect((await platform.api.merchants.get(merchantBetaId)).status).toBe(200)
    expect((await tenant.api.merchants.get(merchantBetaId)).status).toBe(404)
    expect(tenantList.body.content.map(row => row.merchantReference)).not.toContain(mrIso.betaReference)
    for (const row of tenantList.body.content) expect((await platform.api.merchants.get(row.merchantId)).status, `MR-ISO ${row.merchantReference}`).toBe(200)
  })

  test('tenant admin GET users is 200 scoped; manager GET users is 403', async ({ actors }) => {
    const tenant = await actors.open('tenantAdmin')
    const manager = await actors.open('merchantManager')
    expect(Array.isArray(expectSuccess(await tenant.api.identity.listUsers(), 200).body.users)).toBe(true)
    expectProblem(expectError(await manager.api.identity.listUsers(), 403).body, 403)
  })
})
