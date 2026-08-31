import { merchantAlphaId } from '../auth/accounts'
import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { expectStatus } from '../api/bff-client'
import { test, expect } from '../fixtures'

test('read-only user can list merchants but cannot create or run lifecycle', { tag: ['@security'] }, async ({ actors }, testInfo) => {
  const manager = await actors.open('merchantManager')
  const { app } = await actors.open('readOnlyUser')
    const created = await manager.api.payments.createOrder(
      merchantAlphaId,
      { amountMinor: 1500, currency: 'PLN', clientOrderReference: uniqueOrderReference(testInfo, 'RO') },
      uniqueIdempotencyKey(testInfo, 'RO'),
    )
    expectStatus(created, 201)

    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await expect(app.merchants.createButton()).toHaveCount(0)
    await expect(app.sidebar.users()).toHaveCount(0)
    await expect(app.sidebar.audit()).toHaveCount(0)

    await app.paymentDetail.gotoOrder(merchantAlphaId, created.body.paymentOrderId!)
    await app.paymentDetail.expectLoaded()
    await expect(app.paymentDetail.lifecycleAction('authorize')).toHaveCount(0)
    await expect(app.paymentDetail.notesForm()).toHaveCount(0)
})

test('PW-OPS-SEC-041 readonly palette has no Create merchant action', { tag: ['@security'] }, async ({ actors }) => {
  const { app } = await actors.open('readOnlyUser')
    await app.merchants.goto()
    await app.merchants.expectRegistryTable()
    await app.commandPalette.openWithKeyboard()
    await app.commandPalette.search('Create merchant')
    await expect(app.commandPalette.dialog().getByRole('option', { name: 'Create merchant' })).toHaveCount(0)
})
