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

  // The simulator is a client-side state flip (no network), so the mutation
  // oracle is the approve click itself: count DOM clicks until the outcome
  // renders and assert exactly one — a retry loop would have produced more.
  const approveClicks = pspPage.evaluate(() => new Promise<number>((resolve) => {
    let clicks = 0
    document.addEventListener('click', (event) => {
      if (event.target instanceof Element && event.target.closest('[data-testid="psp-approve"]')) {
        clicks += 1
      }
    }, { capture: true })
    const observer = new MutationObserver(() => {
      if (document.querySelector('[data-testid="psp-outcome"]')) {
        observer.disconnect()
        resolve(clicks)
      }
    })
    observer.observe(document.body, { childList: true, subtree: true })
  }))
  await simulator.approve()
  expect(await approveClicks, 'approve must be clicked exactly once').toBe(1)
  await expect(simulator.outcome()).toContainText('Payment approved')
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
