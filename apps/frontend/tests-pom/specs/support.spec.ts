import { merchantBetaId } from '../auth/accounts'
import { test, expect } from '../fixtures'

test('merchant manager support search on Beta is denied', { tag: ['@security'] }, async ({ app }) => {
  await app.page.goto('/admin/merchants')
  await expect(app.sidebar.support()).toHaveCount(0)

  await app.support.goto()
  await app.support.expectLoaded()
  await app.support.search(merchantBetaId)
  await expect(app.support.errorState()).toBeVisible()
  await app.problem.expectVisible()
  await expect(app.support.resultsTable()).toHaveCount(0)
})
