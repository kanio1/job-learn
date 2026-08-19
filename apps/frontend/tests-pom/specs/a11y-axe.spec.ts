import AxeBuilder from '@axe-core/playwright'
import { test, expect } from '../fixtures'
import { App } from '../pages/App'

async function expectNoSeriousAxeViolations(page: import('@playwright/test').Page) {
  const results = await new AxeBuilder({ page })
    .exclude('nuxt-devtools-frame')
    .disableRules(['color-contrast'])
    .analyze()
  const serious = results.violations.filter(v => v.impact === 'serious' || v.impact === 'critical')
  expect(serious, JSON.stringify(serious, null, 2)).toEqual([])
}

test('login page has no serious axe violations', { tag: ['@a11y'] }, async ({ browser }) => {
  const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
  const page = await context.newPage()
  const guest = new App(page)
  try {
    await guest.login.goto()
    await guest.login.expectLoaded()
    await expectNoSeriousAxeViolations(page)
  } finally {
    await context.close()
  }
})

test('merchants registry has no serious axe violations', { tag: ['@a11y'] }, async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expectNoSeriousAxeViolations(app.page)
})
