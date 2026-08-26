import type { TestInfo } from '@playwright/test'
import { BffClient, type Playwright , expectStatus } from '../api/bff-client'
import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'

async function createEurPayment(playwright: Playwright, testInfo: TestInfo) {
  const api = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  try {
    const created = await api.createPaymentOrder(
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
  finally {
    await api.dispose()
  }
}

test.describe('PW-OPS-E2E-210 pl-PL', () => {
  test.use({ locale: 'pl-PL', timezoneId: 'Europe/Warsaw' })

  test('pl-PL payment amount regex', async ({ app, playwright }, testInfo) => {
    const paymentOrderId = await createEurPayment(playwright, testInfo)
    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.paymentDetail.amount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  })
})

test.describe('PW-OPS-E2E-211 sv-SE', () => {
  test.use({ locale: 'sv-SE', timezoneId: 'Europe/Stockholm' })

  test('sv-SE date 2026-08-20', async ({ app, playwright }, testInfo) => {
    const paymentOrderId = await createEurPayment(playwright, testInfo)
    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.localeSelect.sampleDate()).toHaveText('2026-08-20')
    await expect(app.paymentDetail.amount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  })
})

test('PW-OPS-E2E-212 switch LocaleSelect EN→PL persist reload', async ({ app, page, playwright }, testInfo) => {
  await app.merchants.goto()
  await app.localeSelect.expectOpen()
  await app.localeSelect.select('English')
  await expect(app.localeSelect.sampleAmount()).toHaveText('€1,234.56')
  await app.localeSelect.select('Polski')
  await expect(app.localeSelect.sampleAmount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  await expect(app.page.getByRole('heading', { name: 'Sprzedawcy' })).toBeVisible()
  await expect(app.page.getByRole('columnheader', { name: 'Referencja' })).toBeVisible()
  await page.reload()
  await app.localeSelect.expectOpen()
  await expect(app.localeSelect.sampleAmount()).toHaveText(/1[\s\u00a0\u202f]234,56/)
  await expect(app.page.getByRole('heading', { name: 'Sprzedawcy' })).toBeVisible()
  await expect.poll(async () => page.evaluate(() => document.documentElement.lang)).toMatch(/^pl/)
  const cookies = await page.context().cookies()
  expect(cookies.some(cookie => cookie.name === 'pq-locale' && cookie.value.includes('pl'))).toBe(true)

  const paymentOrderId = await createEurPayment(playwright, testInfo)
  await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
  await app.paymentDetail.expectLoaded()
  await expect(app.page.getByTestId('payment-order-detail').getByRole('heading', { name: 'Szczegóły płatności' })).toBeVisible()
  await expect(app.page.getByText('Waluta', { exact: true })).toBeVisible()

  await app.support.goto()
  await expect(app.page.getByRole('tab', { name: 'Kolejka' })).toBeVisible()
})

test.describe('PW-OPS-E2E-213 en-US', () => {
  test.use({ locale: 'en-US', timezoneId: 'America/New_York' })

  test('en-US amount €1,234.56', async ({ app, playwright }, testInfo) => {
    const paymentOrderId = await createEurPayment(playwright, testInfo)
    await app.paymentDetail.gotoOrder(merchantAlphaId, paymentOrderId)
    await app.paymentDetail.expectLoaded()
    await expect(app.paymentDetail.amount()).toHaveText('€1,234.56')
  })
})

test('readonly still has no Save', async ({ browser }) => {
  const context = await browser.newContext({ storageState: pomAuthFiles.readOnlyUser })
  const page = await context.newPage()
  const app = new App(page)
  try {
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(page.getByTestId('merchant-save')).toHaveCount(0)
    await expect(page.getByTestId('tenant-settings-save')).toHaveCount(0)
    await expect(page.getByRole('button', { name: /save|zapisz|spara/i })).toHaveCount(0)
  }
  finally {
    await context.close()
  }
})
