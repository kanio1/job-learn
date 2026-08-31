import { merchantBetaId } from '../auth/accounts'
import { test, expect } from '../fixtures'

test('platform admin support search on Beta returns results', { tag: ['@security'] }, async ({ app }) => {
  await app.support.goto()
  await app.support.expectLoaded()
  await app.support.search(merchantBetaId)
  await expect(app.support.errorState()).toHaveCount(0)
  await expect(app.support.results()).toBeVisible()
  await expect(app.support.resultsSummary()).toBeVisible()
  await expect(app.support.resultsTable()).toBeVisible()
})
