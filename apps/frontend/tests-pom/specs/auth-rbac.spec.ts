import { expect, test } from '../fixtures'
import { merchantAlphaId, merchantBetaId } from '../auth/accounts'
import { expectProblem } from '../utils/http'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

test('real roles see Alpha payments; merchant manager is denied Beta and Users', async ({ actors }) => {
  const platformActor = await actors.open('platformAdmin')
  const managerActor = await actors.open('merchantManager')
  const { page: platformPage, app: platform } = platformActor
  const { page: managerPage, app: manager } = managerActor

    await platform.payments.gotoForMerchant(merchantAlphaId)
    await manager.payments.gotoForMerchant(merchantAlphaId)

    await platform.payments.expectLoaded()
    await manager.payments.expectLoaded()
    await expect(platformPage.getByText(/[1-9]\d* order\(s\) across/)).toBeVisible()
    await expect(managerPage.getByText(/[1-9]\d* order\(s\) across/)).toBeVisible()

    await expect(platform.sidebar.users()).toBeVisible()
    await expect(platform.sidebar.audit()).toBeVisible()
    await expect(manager.sidebar.users()).toHaveCount(0)
    await expect(manager.sidebar.audit()).toHaveCount(0)
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
})
