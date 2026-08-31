import type { TestInfo } from '@playwright/test'
import { type BffClient, expectStatus } from '../api/bff-client'
import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'

async function createEurPayment(api: BffClient, testInfo: TestInfo) {
  const created = await api.payments.createOrder(
      merchantAlphaId,
      {
        amountMinor: 123456,
        currency: 'EUR',
        clientOrderReference: uniqueOrderReference(testInfo, 'LOC'),
      },
      uniqueIdempotencyKey(testInfo, 'LOC'),
    )
  expectStatus(created, 201)
  return created.body.paymentOrderId!
}

test.describe('PW-OPS-E2E-210 pl-PL', () => {
  test.use({ locale: 'pl-PL', timezoneId: 'Europe/Warsaw' })

  test('pl-PL payment amount regex', async ({ app, actors }, testInfo) => {
    const paymentOrderId = await createEurPayment((await actors.open('merchantManager')).api, testInfo)
    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.paymentDetail.amount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  })
})

test.describe('PW-OPS-E2E-211 sv-SE', () => {
  test.use({ locale: 'sv-SE', timezoneId: 'Europe/Stockholm' })

  test('sv-SE date 2026-08-20', async ({ app, actors }, testInfo) => {
    const paymentOrderId = await createEurPayment((await actors.open('merchantManager')).api, testInfo)
    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.localeSelect.sampleDate()).toHaveText('2026-08-20')
    await expect(app.paymentDetail.amount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  })
})

test('PW-OPS-E2E-212 switch LocaleSelect EN→PL persist reload', async ({ app, page, actors }, testInfo) => {
  await app.merchants.goto()
  await app.localeSelect.expectOpen()
  await app.localeSelect.select('English')
  await expect(app.localeSelect.sampleAmount()).toHaveText('€1,234.56')
  await app.localeSelect.select('Polski')
  await expect(app.localeSelect.sampleAmount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  await expect(app.merchants.heading('Sprzedawcy')).toBeVisible()
  await expect(app.merchants.columnHeader('Referencja')).toBeVisible()
  await page.reload()
  await app.localeSelect.expectOpen()
  await expect(app.localeSelect.sampleAmount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  await expect(app.merchants.heading('Sprzedawcy')).toBeVisible()
  await expect.poll(async () => page.evaluate(() => document.documentElement.lang)).toMatch(/^pl/)
  const cookies = await page.context().cookies()
  expect(cookies.some(cookie => cookie.name === 'pq-locale' && cookie.value.includes('pl'))).toBe(true)

  const paymentOrderId = await createEurPayment((await actors.open('merchantManager')).api, testInfo)
  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await expect(app.paymentDetail.heading('Szczegóły płatności')).toBeVisible()
  await expect(app.paymentDetail.fieldLabel('Waluta')).toBeVisible()

  await app.support.goto()
  await expect(app.support.queueTab()).toBeVisible()
})

test.describe('PW-OPS-E2E-213 en-US', () => {
  test.use({ locale: 'en-US', timezoneId: 'America/New_York' })

  test('en-US amount €1,234.56', async ({ app, actors }, testInfo) => {
    const paymentOrderId = await createEurPayment((await actors.open('merchantManager')).api, testInfo)
    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.paymentDetail.amount()).toHaveText('€1,234.56')
  })
})

test('readonly still has no Save', async ({ actors }) => {
  const { app } = await actors.open('readOnlyUser')
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(app.merchants.saveButton()).toHaveCount(0)
    await expect(app.tenantSettings.saveButton()).toHaveCount(0)
    await expect(app.merchants.saveLikeButtons()).toHaveCount(0)
})
