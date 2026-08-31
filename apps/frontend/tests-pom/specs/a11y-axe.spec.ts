import AxeBuilder from '@axe-core/playwright'
import { test, expect } from '../fixtures'

async function expectNoSeriousAxeViolations(page: import('@playwright/test').Page) {
  const results = await new AxeBuilder({ page })
    .exclude('nuxt-devtools-frame')
    .disableRules(['color-contrast'])
    .analyze()
  const serious = results.violations.filter(v => v.impact === 'serious' || v.impact === 'critical')
  expect(serious, JSON.stringify(serious, null, 2)).toEqual([])
}

test('login page has no serious axe violations', { tag: ['@a11y'] }, async ({ actors }) => {
  const guest = await actors.open('guest')
  await guest.app.login.goto()
  await guest.app.login.expectLoaded()
  await expectNoSeriousAxeViolations(guest.page)
})

test('merchants registry has no serious axe violations', { tag: ['@a11y'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expectNoSeriousAxeViolations(app.page)
})
