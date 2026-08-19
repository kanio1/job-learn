import { expect, test } from '../fixtures'
import { merchantAlphaId, merchantBetaId } from '../auth/accounts'
import { pomAuthFiles } from '../utils/env'
import { expectProblem } from '../utils/http'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { App } from '../pages/App'

test('real roles see Alpha payments; merchant manager is denied Beta and Users', async ({ browser }) => {
  const platformContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const managerContext = await browser.newContext({ storageState: pomAuthFiles.merchantManager })
  const platformPage = await platformContext.newPage()
  const managerPage = await managerContext.newPage()
  const platform = new App(platformPage)
  const manager = new App(managerPage)

  try {
    await platform.payments.gotoForMerchant(merchantAlphaId)
    await manager.payments.gotoForMerchant(merchantAlphaId)

    await platform.payments.expectLoaded()
    await manager.payments.expectLoaded()
    await expect(platformPage.getByText(/[1-9]\d* order\(s\) across/)).toBeVisible()
    await expect(managerPage.getByText(/[1-9]\d* order\(s\) across/)).toBeVisible()

    await platform.sidebar.expectUsersVisible(true)
    await platform.sidebar.expectAuditVisible(true)
    await manager.sidebar.expectUsersVisible(false)
    await manager.sidebar.expectAuditVisible(false)
    await expectNoTokenInBrowserStorage(platformPage)
    await expectNoTokenInBrowserStorage(managerPage)

    await manager.payments.gotoForMerchant(merchantBetaId)
    const forbiddenAlert = managerPage.getByRole('alert').filter({
      hasText: 'You do not have permission to view payment orders',
    })
    await expect(forbiddenAlert).toBeVisible()
    await expect(managerPage.getByTestId('payment-orders-table')).not.toBeVisible()

    await manager.users.goto()
    await manager.users.expectForbidden()
    const usersResponse = await managerPage.request.get('/api/users')
    expect(usersResponse.status()).toBe(403)
    expect(usersResponse.status()).not.toBe(502)
    expectProblem(await usersResponse.json(), 403)
  } finally {
    await platformContext.close()
    await managerContext.close()
  }
})
