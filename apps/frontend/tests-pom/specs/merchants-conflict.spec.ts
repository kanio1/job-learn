import { uniqueMerchantReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { etagOf, expectProblem } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'

test.describe('Merchant contact conflict', { tag: ['@security'] }, () => {
  test('PW-OPS-API-010 BFF cookie forwards If-Match and ETag on contact PATCH', async ({ api }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Api ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!
    const etag = etagOf(created.headers)!
    const patched = await client.merchants.patch(merchantId, { contactPhone: '+48111111111' }, etag)
    expectStatus(patched, 200)
    expect(etagOf(patched.headers)).toBeTruthy()
    expect(etagOf(patched.headers)).not.toBe(etag)
    const stale = await client.merchants.patch(merchantId, { contactAddress: 'Nope' }, etag)
    expectStatus(stale, 412)
    expectProblem(stale.body, 412, 'merchant_version_mismatch')
  })

  test('PW-OPS-SEC-020 two contexts stale contact PATCH shows Your vs Latest', async ({ api, actors }, testInfo) => {
    const seed = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.merchants.create(reference, `Race ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const admin = await actors.open('platformAdmin')
    const operator = await actors.open('platformOperator')
      await admin.app.merchantDetail.gotoMerchant(merchantId)
      await admin.app.merchantDetail.expectLoaded()
      await admin.app.merchantDetail.fillContact({ contactPhone: '+48111111111' })

      const current = await operator.api.merchants.get(merchantId)
      expectStatus(current, 200)
      const operatorSave = await operator.api.merchants.patch(
        merchantId,
        { contactAddress: 'Operator Street' },
        etagOf(current.headers)!,
      )
      expectStatus(operatorSave, 200)

      const patch = waitForBffResponse(admin.page, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await admin.app.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await admin.app.merchantDetail.conflict.expectOpen()
  })

  test('PW-OPS-SEC-021 Discard mine applies server without second stale PATCH', async ({ api, actors }, testInfo) => {
    const seed = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.merchants.create(reference, `Discard ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const admin = await actors.open('platformAdmin')
    const operator = await actors.open('platformOperator')
      await admin.app.merchantDetail.gotoMerchant(merchantId)
      await admin.app.merchantDetail.expectLoaded()
      await admin.app.merchantDetail.fillContact({ contactPhone: '+48111111111' })

      const current = await operator.api.merchants.get(merchantId)
      expect((await operator.api.merchants.patch(
        merchantId,
        { contactAddress: 'Server Address' },
        etagOf(current.headers)!,
      )).status).toBe(200)

      const patch = waitForBffResponse(admin.page, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await admin.app.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await admin.app.merchantDetail.conflict.expectOpen()

      let extraPatch = 0
      admin.page.on('request', (request) => {
        try {
          if (request.method() === 'PATCH' && new URL(request.url()).pathname === `/api/merchants/${merchantId}`) {
            extraPatch += 1
          }
        }
        catch { /* ignore */ }
      })
      await admin.app.merchantDetail.conflict.discardMine()
      expect(extraPatch).toBe(0)
      await expect(admin.app.merchantDetail.phoneInput()).toHaveValue('')
  })

  test('PW-OPS-E2E-130 conflict tabs Your and Latest after 412', async ({ api, actors }, testInfo) => {
    const seed = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.merchants.create(reference, `Tabs ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const admin = await actors.open('platformAdmin')
    const operator = await actors.open('platformOperator')
      await admin.app.merchantDetail.gotoMerchant(merchantId)
      await admin.app.merchantDetail.expectLoaded()
      await admin.app.merchantDetail.fillContact({ contactPhone: '+48111111111' })
      const current = await operator.api.merchants.get(merchantId)
      expect((await operator.api.merchants.patch(
        merchantId,
        { contactPhone: '+48222222222' },
        etagOf(current.headers)!,
      )).status).toBe(200)
      const patch = waitForBffResponse(admin.page, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await admin.app.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await admin.app.merchantDetail.conflict.expectOpen()
      await expect(admin.app.merchantDetail.conflict.yours()).toContainText('+48111111111')
      await admin.app.merchantDetail.conflict.openLatest()
      await expect(admin.app.merchantDetail.conflict.latest()).toContainText('+48222222222')
  })

  test('PW-OPS-E2E-131 Escape closes conflict without PATCH', async ({ api, actors }, testInfo) => {
    const seed = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.merchants.create(reference, `Esc ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const admin = await actors.open('platformAdmin')
    const operator = await actors.open('platformOperator')
      await admin.app.merchantDetail.gotoMerchant(merchantId)
      await admin.app.merchantDetail.expectLoaded()
      await admin.app.merchantDetail.fillContact({ contactPhone: '+48111111111' })
      const current = await operator.api.merchants.get(merchantId)
      expect((await operator.api.merchants.patch(
        merchantId,
        { contactPhone: '+48222222222' },
        etagOf(current.headers)!,
      )).status).toBe(200)
      const patch = waitForBffResponse(admin.page, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await admin.app.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await admin.app.merchantDetail.conflict.expectOpen()
      await admin.page.keyboard.press('Escape')
      await expect(admin.app.merchantDetail.conflict.dialog()).toBeHidden()
      await expect(admin.app.merchantDetail.phoneInput()).toHaveValue('+48111111111')
  })

  test('PW-OPS-E2E-132 aria snapshot conflict dialog', async ({ api, actors }, testInfo) => {
    const seed = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await seed.merchants.create(reference, `Aria ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    const admin = await actors.open('platformAdmin')
    const operator = await actors.open('platformOperator')
      await admin.app.merchantDetail.gotoMerchant(merchantId)
      await admin.app.merchantDetail.expectLoaded()
      await admin.app.merchantDetail.fillContact({ contactPhone: '+48111111111' })
      const current = await operator.api.merchants.get(merchantId)
      expect((await operator.api.merchants.patch(
        merchantId,
        { contactPhone: '+48222222222' },
        etagOf(current.headers)!,
      )).status).toBe(200)
      const patch = waitForBffResponse(admin.page, {
        method: 'PATCH',
        pathExact: `/api/merchants/${merchantId}`,
      })
      await admin.app.merchantDetail.saveContact()
      expect((await patch).status()).toBe(412)
      await admin.app.merchantDetail.conflict.expectOpen()
      await expect(admin.app.merchantDetail.conflict.dialog()).toMatchAriaSnapshot(`
        - text: Record changed by another user. Your save was not applied.
        - tablist:
          - tab "Your changes" [selected]
          - tab "Latest version"
        - button "Discard mine"
        - button "Reload latest"
      `)
  })
})
