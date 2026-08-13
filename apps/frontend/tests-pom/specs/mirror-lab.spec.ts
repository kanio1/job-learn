import { test, expect } from '../fixtures'

test('mirror hub and bank statements download', async ({ app }) => {
  await app.mirrorHub.goto()
  await app.mirrorHub.expectLoaded()
  await app.mirrorHub.openBank()
  await app.mirrorBank.expectLoaded()
  const downloadPromise = app.page.waitForEvent('download')
  await app.page.getByTestId('statement-download-csv').click()
  const download = await downloadPromise
  expect(download.suggestedFilename()).toMatch(/statement/i)
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
