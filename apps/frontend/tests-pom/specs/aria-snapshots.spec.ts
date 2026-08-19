import { merchantAlphaId } from '../auth/accounts'
import { pomAuthFiles } from '../utils/env'
import { test, expect } from '../fixtures'
import { App } from '../pages/App'

test('create-merchant form matches ARIA snapshot', { tag: ['@visual'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.merchants.openCreateForm()
  await expect(app.page.getByTestId('create-merchant-form')).toMatchAriaSnapshot()
})

test('create-payment form matches ARIA snapshot', { tag: ['@visual'] }, async ({ browser }) => {
  const context = await browser.newContext({ storageState: pomAuthFiles.merchantManager })
  const page = await context.newPage()
  const manager = new App(page)
  try {
    await manager.paymentCreate.gotoForMerchant(merchantAlphaId)
    await manager.paymentCreate.expectLoaded()
    await expect(page.getByTestId('create-payment-order-form')).toMatchAriaSnapshot()
  } finally {
    await context.close()
  }
})
