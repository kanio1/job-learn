import { test, expect } from '../fixtures'

test('payment list shows the offline banner when the browser loses network', async ({ app, context, ownedMerchantId }) => {
  await app.payments.gotoForMerchant(ownedMerchantId)
  await app.payments.expectLoaded()

  await context.setOffline(true)
  await app.page.evaluate(() => window.dispatchEvent(new Event('offline')))
  await expect(app.page.getByTestId('payments-offline-banner')).toBeVisible()

  await context.setOffline(false)
  await app.page.evaluate(() => window.dispatchEvent(new Event('online')))
  await expect(app.page.getByTestId('payments-offline-banner')).toHaveCount(0)
})
