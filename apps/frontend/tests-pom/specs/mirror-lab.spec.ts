import { readFileSync } from 'node:fs'
import { test, expect } from '../fixtures'

test('mirror hub and bank statements download', async ({ app }) => {
  await app.mirrorHub.goto()
  await app.mirrorHub.expectLoaded()
  await app.mirrorHub.openBank()
  await app.mirrorBank.expectLoaded()
  const download = await app.mirrorBank.downloadCsv()
  expect(download.suggestedFilename()).toMatch(/statement/i)
})

test('bank statement PDF download starts with PDF magic bytes', async ({ app }) => {
  await app.mirrorBank.goto()
  await app.mirrorBank.expectLoaded()
  const download = await app.mirrorBank.downloadPdf()
  expect(download.suggestedFilename()).toMatch(/\.pdf$/i)
  const path = await download.path()
  expect(path, 'Playwright must keep the PDF download on disk').toBeTruthy()
  const header = readFileSync(path!).subarray(0, 5).toString('latin1')
  expect(header).toBe('%PDF-')
})

test('expired hosted checkout exposes test id', async ({ app, context }) => {
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.page.getByTestId('checkout-booking-scenario').click()
  await app.page.getByRole('option', { name: 'EXPIRED_LINK' }).click()
  await app.checkoutBooking.fillExtOrderId(`EXP-${Date.now()}`)
  await app.checkoutBooking.submit()
  const hostedPromise = context.waitForEvent('page')
  await app.page.getByTestId('checkout-open-hosted').click()
  const hostedPage = await hostedPromise
  await expect(hostedPage.getByTestId('psp-link-expired')).toBeVisible()
})

test('widget iframe is same-origin', async ({ app }) => {
  await app.checkoutHub.goto()
  await app.checkoutHub.expectLoaded()
  await app.checkoutHub.openWidget()
  await expect(app.page.getByTestId('widget-session-id')).toBeVisible()
})
