import { merchantBetaId } from '../auth/accounts'
import { test, expect } from '../fixtures'

test('platform admin support search on Beta returns results', { tag: ['@security'] }, async ({ app }) => {
  await app.support.goto()
  await app.support.expectLoaded()
  await app.support.search(merchantBetaId)
  await expect(app.page.getByTestId('error-state')).toHaveCount(0)
  await app.support.expectResults()
})
