import { uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { etagOf, expectProblem } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { openAdminAndOperator } from '../fixtures/multi-user.fixture'

test.describe('Merchant contact conflict', { tag: ['@security'] }, () => {
  test('PW-OPS-API-010 BFF cookie forwards If-Match and ETag on contact PATCH', async ({ api }, testInfo) => {
    const client = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.createMerchant(reference, `Api ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!
    const etag = etagOf(created.headers)!
    const patched = await client.patchMerchant(merchantId, { contactPhone: '+48111111111' }, etag)
    expectStatus(patched, 200)
    expect(etagOf(patched.headers)).toBeTruthy()
    expect(etagOf(patched.headers)).not.toBe(etag)
    const stale = await client.patchMerchant(merchantId, { contactAddress: 'Nope' }, etag)
    expectStatus(stale, 412)
    expectProblem(stale.body, 412, 'merchant_version_mismatch')
  })

  test('PW-OPS-SEC-020 two contexts stale contact PATCH shows Your vs Latest', async ({
    browser,
    playwright,
    api,
  }, testInfo) => {
    const seed = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.createMerchant(reference, `Race ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const sessions = await openAdminAndOperator(browser, playwright)
    try {
      await sessions.adminApp.merchantDetail.gotoMerchant(merchantId)
      await sessions.adminApp.merchantDetail.expectLoaded()
      await sessions.adminApp.merchantDetail.fillContact({ contactPhone: '+48111111111' })

      const current = await sessions.operatorApi.getMerchant(merchantId)
      expectStatus(current, 200)
      const operatorSave = await sessions.operatorApi.patchMerchant(
        merchantId,
        { contactAddress: 'Operator Street' },
        etagOf(current.headers)!,
      )
      expectStatus(operatorSave, 200)

      const patch = waitForBffResponse(sessions.adminPage, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await sessions.adminApp.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await sessions.adminApp.merchantDetail.conflict.expectOpen()
    }
    finally {
      await sessions.close()
    }
  })

  test('PW-OPS-SEC-021 Discard mine applies server without second stale PATCH', async ({
    browser,
    playwright,
    api,
  }, testInfo) => {
    const seed = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.createMerchant(reference, `Discard ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const sessions = await openAdminAndOperator(browser, playwright)
    try {
      await sessions.adminApp.merchantDetail.gotoMerchant(merchantId)
      await sessions.adminApp.merchantDetail.expectLoaded()
      await sessions.adminApp.merchantDetail.fillContact({ contactPhone: '+48111111111' })

      const current = await sessions.operatorApi.getMerchant(merchantId)
      expect((await sessions.operatorApi.patchMerchant(
        merchantId,
        { contactAddress: 'Server Address' },
        etagOf(current.headers)!,
      )).status).toBe(200)

      const patch = waitForBffResponse(sessions.adminPage, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await sessions.adminApp.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await sessions.adminApp.merchantDetail.conflict.expectOpen()

      let extraPatch = 0
      sessions.adminPage.on('request', (request) => {
        try {
          if (request.method() === 'PATCH' && new URL(request.url()).pathname === `/api/merchants/${merchantId}`) {
            extraPatch += 1
          }
        }
        catch { /* ignore */ }
      })
      await sessions.adminApp.merchantDetail.conflict.discardMine()
      expect(extraPatch).toBe(0)
      await expect(sessions.adminApp.merchantDetail.phoneInput()).toHaveValue('')
    }
    finally {
      await sessions.close()
    }
  })

  test('PW-OPS-E2E-130 conflict tabs Your and Latest after 412', async ({ browser, playwright, api }, testInfo) => {
    const seed = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.createMerchant(reference, `Tabs ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const sessions = await openAdminAndOperator(browser, playwright)
    try {
      await sessions.adminApp.merchantDetail.gotoMerchant(merchantId)
      await sessions.adminApp.merchantDetail.expectLoaded()
      await sessions.adminApp.merchantDetail.fillContact({ contactPhone: '+48111111111' })
      const current = await sessions.operatorApi.getMerchant(merchantId)
      expect((await sessions.operatorApi.patchMerchant(
        merchantId,
        { contactPhone: '+48222222222' },
        etagOf(current.headers)!,
      )).status).toBe(200)
      const patch = waitForBffResponse(sessions.adminPage, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await sessions.adminApp.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await sessions.adminApp.merchantDetail.conflict.expectOpen()
      await expect(sessions.adminPage.getByTestId('conflict-yours')).toContainText('+48111111111')
      await sessions.adminPage.getByRole('tab', { name: /latest version/i }).click()
      await expect(sessions.adminPage.getByTestId('conflict-latest')).toContainText('+48222222222')
    }
    finally {
      await sessions.close()
    }
  })

  test('PW-OPS-E2E-131 Escape closes conflict without PATCH', async ({ browser, playwright, api }, testInfo) => {
    const seed = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.createMerchant(reference, `Esc ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const sessions = await openAdminAndOperator(browser, playwright)
    try {
      await sessions.adminApp.merchantDetail.gotoMerchant(merchantId)
      await sessions.adminApp.merchantDetail.expectLoaded()
      await sessions.adminApp.merchantDetail.fillContact({ contactPhone: '+48111111111' })
      const current = await sessions.operatorApi.getMerchant(merchantId)
      expect((await sessions.operatorApi.patchMerchant(
        merchantId,
        { contactPhone: '+48222222222' },
        etagOf(current.headers)!,
      )).status).toBe(200)
      const patch = waitForBffResponse(sessions.adminPage, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await sessions.adminApp.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await sessions.adminApp.merchantDetail.conflict.expectOpen()
      await sessions.adminPage.keyboard.press('Escape')
      await expect(sessions.adminApp.merchantDetail.conflict.dialog()).toBeHidden()
      await expect(sessions.adminApp.merchantDetail.phoneInput()).toHaveValue('+48111111111')
    }
    finally {
      await sessions.close()
    }
  })

  test('PW-OPS-E2E-132 aria snapshot conflict dialog', async ({ browser, playwright, api }, testInfo) => {
    const seed = requireApi(api)
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.createMerchant(reference, `Aria ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const sessions = await openAdminAndOperator(browser, playwright)
    try {
      await sessions.adminApp.merchantDetail.gotoMerchant(merchantId)
      await sessions.adminApp.merchantDetail.expectLoaded()
      await sessions.adminApp.merchantDetail.fillContact({ contactPhone: '+48111111111' })
      const current = await sessions.operatorApi.getMerchant(merchantId)
      expect((await sessions.operatorApi.patchMerchant(
        merchantId,
        { contactPhone: '+48222222222' },
        etagOf(current.headers)!,
      )).status).toBe(200)
      const patch = waitForBffResponse(sessions.adminPage, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await sessions.adminApp.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await sessions.adminApp.merchantDetail.conflict.expectOpen()
      await expect(sessions.adminApp.merchantDetail.conflict.dialog()).toMatchAriaSnapshot(`
        - text: Record changed by another user. Your save was not applied.
        - tablist:
          - tab "Your changes" [selected]
          - tab "Latest version"
        - button "Discard mine"
        - button "Reload latest"
      `)
    }
    finally {
      await sessions.close()
    }
  })
})
