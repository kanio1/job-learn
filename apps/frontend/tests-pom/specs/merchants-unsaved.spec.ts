import { uniqueMerchantReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'

test.describe('Merchant unsaved guard', () => {
  test('PW-OPS-E2E-160 goBack Stay keeps edit URL', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Dirty ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchantDetail.gotoMerchant(merchantId)
    await app.merchantDetail.expectLoaded()
    await app.merchantDetail.fillContact({ contactPhone: '+48111111111' })
    await page.goBack()
    await app.merchantDetail.unsaved.expectOpen()
    await app.merchantDetail.unsaved.stay()
    await expect(page).toHaveURL(new RegExp(`/admin/merchants/${merchantId}`))
    await expect(app.merchantDetail.unsaved.dialog()).toBeHidden()
  })

  test('PW-OPS-E2E-161 NuxtLink Discard goes to list', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Leave ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchantDetail.gotoMerchant(merchantId)
    await app.merchantDetail.expectLoaded()
    await app.merchantDetail.fillContact({ contactPhone: '+48111111111' })
    await app.merchantDetail.goBackToList()
    await app.merchantDetail.unsaved.expectOpen()
    await app.merchantDetail.unsaved.discard()
    await expect(page).toHaveURL(/\/admin\/merchants\/?$/)
  })

  test('PW-OPS-E2E-162 clean form Back has no dialog', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Clean ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchantDetail.gotoMerchant(merchantId)
    await app.merchantDetail.expectLoaded()
    await app.merchantDetail.goBackToList()
    await expect(app.merchantDetail.unsaved.dialog()).toHaveCount(0)
    await expect(page).toHaveURL(/\/admin\/merchants\/?$/)
  })

  test('PW-OPS-E2E-163 dirty close runBeforeUnload is beforeunload', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Unload ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchantDetail.gotoMerchant(merchantId)
    await app.merchantDetail.expectLoaded()
    await app.merchantDetail.fillContact({ contactPhone: '+48111111111' })

    const dialogPromise = page.waitForEvent('dialog')
    const closing = page.close({ runBeforeUnload: true })
    const dialog = await dialogPromise
    expect(dialog.type()).toBe('beforeunload')
    await dialog.dismiss()
    await closing
  })

  test('PW-OPS-E2E-164 Stay sends zero PATCH', async ({ app, api, page }, testInfo) => {
    const client = api
    const reference = uniqueMerchantReference(testInfo)
    const created = await client.merchants.create(reference, `Zero ${reference}`)
    expectStatus(created, 201)
    const merchantId = created.body.merchantId!

    await app.merchants.goto()
    await app.merchants.expectLoaded()
    await app.merchantDetail.gotoMerchant(merchantId)
    await app.merchantDetail.expectLoaded()
    await app.merchantDetail.fillContact({ contactPhone: '+48111111111' })

    const patches: string[] = []
    page.on('request', (request) => {
      if (request.method() !== 'PATCH') {
        return
      }
      try {
        if (new URL(request.url()).pathname === `/api/merchants/${merchantId}`) {
          patches.push(request.url())
        }
      }
      catch { /* ignore */ }
    })
    await page.goBack()
    await app.merchantDetail.unsaved.expectOpen()
    await app.merchantDetail.unsaved.stay()
    expect(patches).toHaveLength(0)
  })
})
