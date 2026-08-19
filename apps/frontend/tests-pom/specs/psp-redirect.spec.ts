import { test, expect } from '../fixtures'
import { App } from '../pages/App'

test('opens PSP simulator in a new tab, approves, and returns', { tag: ['@ux'] }, async ({ app, context }) => {
  await app.errorLab.goto()
  await app.errorLab.expectLoaded()
  await expect(app.page.getByTestId('psp-redirect-trigger')).toBeVisible()

  const newPagePromise = context.waitForEvent('page')
  await app.page.getByTestId('psp-redirect-trigger').click()
  const pspPage = await newPagePromise
  const simulator = new App(pspPage).pspSimulator
  await simulator.expectLoaded()
  await simulator.approve()
  await expect(app.page.getByTestId('psp-redirect-trigger')).toBeVisible()
  await pspPage.close()
})

test('PSP simulator is reachable without an authenticated session', async ({ browser }) => {
  const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
  const page = await context.newPage()
  const guest = new App(page)
  try {
    await page.goto('/psp-redirect-simulator')
    await expect(page.getByTestId('psp-redirect-simulator')).toBeVisible()
    await expect(page).toHaveURL(/\/psp-redirect-simulator$/)
    await expect(guest.page.getByTestId('psp-approve')).toBeVisible()
  } finally {
    await context.close()
  }
})
