import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { BffClient } from '../api/bff-client'
import { pomAuthFiles } from '../utils/env'
import { test, expect } from '../fixtures'
import { App } from '../pages/App'

test('read-only user can list merchants but cannot create or run lifecycle', { tag: ['@security'] }, async ({ browser, playwright }, testInfo) => {
  const managerApi = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  const context = await browser.newContext({ storageState: pomAuthFiles.readOnlyUser })
  const page = await context.newPage()
  const app = new App(page)
  try {
    const created = await managerApi.createPaymentOrder(
      merchantAlphaId,
      { amountMinor: 1500, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'RO') },
      uniqueIdempotencyKey(testInfo, 'RO'),
    )
    expect(created.status).toBe(201)

    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(page.getByTestId('action-create-merchant')).toHaveCount(0)
    await app.sidebar.expectUsersVisible(false)
    await app.sidebar.expectAuditVisible(false)

    await app.paymentDetail.gotoOrder(merchantAlphaId, created.body.paymentOrderId!)
    await app.paymentDetail.expectLoaded()
    await expect(page.getByTestId('lifecycle-authorize')).toHaveCount(0)
    await expect(page.getByTestId('payment-note-body')).toHaveCount(0)
  } finally {
    await context.close()
    await managerApi.dispose()
  }
})

test('PW-OPS-SEC-041 readonly palette has no Create merchant action', { tag: ['@security'] }, async ({ browser }) => {
  const context = await browser.newContext({ storageState: pomAuthFiles.readOnlyUser })
  const page = await context.newPage()
  const app = new App(page)
  try {
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await app.commandPalette.openWithKeyboard()
    await app.commandPalette.search('Create merchant')
    await expect(app.commandPalette.dialog().getByRole('option', { name: 'Create merchant' })).toHaveCount(0)
  }
  finally {
    await context.close()
  }
})
