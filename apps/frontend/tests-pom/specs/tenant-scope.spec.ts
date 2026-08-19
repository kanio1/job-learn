import { expect, test } from '../fixtures'
import {
  merchantAlphaId,
  merchantAlphaReference,
  merchantAlphaTwoId,
  merchantBetaId,
  merchantBetaReference,
} from '../auth/accounts'
import { BffClient } from '../api/bff-client'
import { uniqueIdempotencyKey, uniqueMerchantReference, uniqueOrderReference } from '../data/factories'
import { PaymentOrderDraft } from '../data/payment-order-draft'
import { pomAuthFiles } from '../utils/env'
import { expectProblem } from '../utils/http'
import { App } from '../pages/App'
import { isolationDtUcE2eRows } from '../methods/combinations/IsolationDtUc'
import { mrIso } from '../methods/metamorphic/IsolationInclusion'
import { createMerchantJourney } from '../methods/use-case/CreateMerchantJourney'

/**
 * SCN-ISO-01 / 03 / 06 / 09 / 10 — DT+UC: RBAC ≠ tenant scope.
 * Contract seed (~104). Does not call seed-learning or ETL.
 */
test.describe('tenant scope vs RBAC @security', () => {
  test('platform admin registry shows Alpha and Beta (SCN-ISO-06)', async ({ browser }) => {
    expect(isolationDtUcE2eRows.some(row => row.id === 'SCN-ISO-06')).toBe(true)
    const context = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
    const page = await context.newPage()
    const app = new App(page)
    try {
      await app.merchantDetail.gotoMerchant(merchantAlphaId)
      await app.merchantDetail.expectLoaded()
      await expect(page.getByTestId('merchant-reference')).toHaveText(merchantAlphaReference)
      await app.merchantDetail.gotoMerchant(merchantBetaId)
      await app.merchantDetail.expectLoaded()
      await expect(page.getByTestId('merchant-reference')).toHaveText(merchantBetaReference)
    }
    finally {
      await context.close()
    }
  })

  test('tenant admin registry shows Alpha and hides Beta (SCN-ISO-01)', async ({ browser, playwright }) => {
    const context = await browser.newContext({ storageState: pomAuthFiles.tenantAdmin })
    const page = await context.newPage()
    const app = new App(page)
    const api = await BffClient.create(playwright, pomAuthFiles.tenantAdmin)
    try {
      await app.merchantDetail.gotoMerchant(merchantAlphaId)
      await app.merchantDetail.expectLoaded()
      await expect(page.getByTestId('merchant-reference')).toHaveText(merchantAlphaReference)
      await app.merchantDetail.gotoMerchant(merchantBetaId)
      await expect(page.getByTestId('merchant-reference')).toHaveCount(0)
      const own = await api.getMerchant(merchantAlphaId)
      const foreign = await api.getMerchant(merchantBetaId)
      expect(own.status).toBe(200)
      expect(own.body?.merchantReference).toBe(merchantAlphaReference)
      expect(foreign.status).toBe(404)
    }
    finally {
      await api.dispose()
      await context.close()
    }
  })

  test('merchant manager registry is RBAC deny not tenant isolation (SCN-ISO-09)', async ({ browser }) => {
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

  test('tenant admin GET Beta is masked 404; GET Alpha is 200 (SCN-ISO-02/03)', async ({ playwright }) => {
    const api = await BffClient.create(playwright, pomAuthFiles.tenantAdmin)
    try {
      const own = await api.getMerchant(merchantAlphaId)
      expect(own.status).toBe(200)
      expect(own.body?.merchantReference).toBe(merchantAlphaReference)

      const foreign = await api.getMerchant(merchantBetaId)
      expect(foreign.status).toBe(404)
      expectProblem(foreign.body, 404)
      const detail = `${foreign.body?.detail ?? ''} ${foreign.body?.title ?? ''}`
      expect(detail.includes('MERCHANT_BETA')).toBe(false)
      expect(detail.includes('PLATFORM_TENANT')).toBe(false)
    }
    finally {
      await api.dispose()
    }
  })

  test('manager POST order on ALPHA_002 is 403 BOLA (SCN-ISO-10)', async ({ playwright }, testInfo) => {
    const api = await BffClient.create(playwright, pomAuthFiles.merchantManager)
    try {
      const created = await api.createPaymentOrder(
        merchantAlphaTwoId,
        PaymentOrderDraft.builder().amount(1000).pln().reference(uniqueOrderReference(testInfo, 'BOLA')).build(),
        uniqueIdempotencyKey(testInfo, 'BOLA'),
      )
      expect(created.status).toBe(403)
    }
    finally {
      await api.dispose()
    }
  })

  test('tenant admin UI create uses JWT tenant and has no tenant field', async ({ browser }, testInfo) => {
    expect(createMerchantJourney.tenantAdminJwt.tenantFieldVisible).toBe(false)
    const context = await browser.newContext({ storageState: pomAuthFiles.tenantAdmin })
    const page = await context.newPage()
    const app = new App(page)
    try {
      await app.merchants.goto()
      await app.merchants.expectLoaded()
      await app.merchants.openCreateForm()
      await expect(page.getByTestId('create-merchant-tenant-reference')).toHaveCount(0)
      const reference = uniqueMerchantReference(testInfo)
      await app.merchants.fillCreateForm(reference, `Tenant create ${reference}`)
      await app.merchants.submitCreate()
      await app.merchants.expectRowVisible(reference)
    } finally {
      await context.close()
    }
  })

  test('MR-ISO: tenant.admin merchants ⊆ platform.admin; Beta excluded', async ({ playwright }) => {
    const tenant = await BffClient.create(playwright, pomAuthFiles.tenantAdmin)
    const platform = await BffClient.create(playwright, pomAuthFiles.platformAdmin)
    try {
      const tenantList = await tenant.listMerchants()
      expect(tenantList.status).toBe(200)
      expect((await platform.getMerchant(merchantBetaId)).status).toBe(200)
      expect((await tenant.getMerchant(merchantBetaId)).status).toBe(404)
      expect((tenantList.body.merchants ?? []).map(row => row.merchantReference))
        .not.toContain(mrIso.betaReference)
      for (const row of tenantList.body.merchants ?? []) {
        expect(
          (await platform.getMerchant(row.merchantId!)).status,
          `MR-ISO ${row.merchantReference}`,
        ).toBe(200)
      }
    }
    finally {
      await tenant.dispose()
      await platform.dispose()
    }
  })
})
