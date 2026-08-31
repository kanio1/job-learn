import { merchantAlphaId } from '../auth/accounts'
import { test, expect } from '../fixtures'

test('create-payment wizard matches ARIA snapshots at every step', { tag: ['@visual'] }, async ({ actors }) => {
  test.setTimeout(60_000)
  const { app: manager } = await actors.open('merchantManager')
    await manager.paymentCreate.gotoForMerchant(merchantAlphaId)
    await manager.paymentCreate.expectLoaded()
    await test.step('amount step', async () => {
      await expect(manager.paymentCreate.form()).toMatchAriaSnapshot()
    })
    await test.step('currency step', async () => {
      await manager.paymentCreate.fillAmount(1200)
      await manager.paymentCreate.next()
      await expect(manager.paymentCreate.form()).toMatchAriaSnapshot()
    })
    await test.step('reference step', async () => {
      await manager.paymentCreate.chooseCurrency('PLN')
      await manager.paymentCreate.next()
      await expect(manager.paymentCreate.form()).toMatchAriaSnapshot()
    })
    await test.step('review step', async () => {
      await manager.paymentCreate.fillReference('ARIA-WIZARD-001')
      await manager.paymentCreate.next()
      await expect(manager.paymentCreate.form()).toMatchAriaSnapshot()
    })
})
