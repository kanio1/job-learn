import { expect, test } from '@playwright/test'
import { liveAuthFiles } from './live-keycloak'
import { expectNoTokenInBrowserStorage } from '../../support/browser-safety-assertions'

const merchantAlphaId = '00000000-0000-0000-0000-0000000000b1'
const merchantBetaId = '00000000-0000-0000-0000-0000000000b3'

test('real roles see their own payment route while merchant manager is denied foreign-tenant data', async ({ browser }) => {
  const platformContext = await browser.newContext({ storageState: liveAuthFiles.platformAdmin })
  const managerContext = await browser.newContext({ storageState: liveAuthFiles.merchantManager })
  const platformPage = await platformContext.newPage()
  const managerPage = await managerContext.newPage()

  try {
    const alphaRoute = `/admin/merchants/${merchantAlphaId}/payments`
    await platformPage.goto(alphaRoute)
    await managerPage.goto(alphaRoute)

    await expect(platformPage.getByTestId('payment-orders-table')).toBeVisible()
    await expect(managerPage.getByTestId('payment-orders-table')).toBeVisible()
    // The live idempotency proof may create an additional Alpha order in a
    // parallel project, so the tenant-visible result must be non-empty rather
    // than tied to the deterministic seed's original absolute count.
    await expect(platformPage.getByText(/[1-9]\d* order\(s\) across/)).toBeVisible()
    await expect(managerPage.getByText(/[1-9]\d* order\(s\) across/)).toBeVisible()

    await expect(platformPage.getByTestId('nav-link-users')).toBeVisible()
    await expect(platformPage.getByTestId('nav-link-audit')).toBeVisible()
    await expect(managerPage.getByTestId('nav-link-users')).not.toBeVisible()
    await expect(managerPage.getByTestId('nav-link-audit')).not.toBeVisible()
    await expectNoTokenInBrowserStorage(platformPage)
    await expectNoTokenInBrowserStorage(managerPage)

    await managerPage.goto(`/admin/merchants/${merchantBetaId}/payments`)
    const forbiddenAlert = managerPage.getByRole('alert').filter({
      hasText: 'You do not have permission to view payment orders',
    })
    await expect(forbiddenAlert).toBeVisible()
    await expect(forbiddenAlert).toContainText('You do not have permission to view payment orders')
    await expect(managerPage.getByTestId('payment-orders-table')).not.toBeVisible()
  } finally {
    await platformContext.close()
    await managerContext.close()
  }
})
