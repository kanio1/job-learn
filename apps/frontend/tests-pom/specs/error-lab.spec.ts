import { test, expect } from '../fixtures'
import { expectProblemStatus, liveErrorLabTrigger } from '../utils/error-lab'

const platformGetStatuses = [401, 403, 404, 406, 304] as const

test.describe('Error Lab live triggers (platform admin)', () => {
  test('Error Lab 401 canary is a live unauthorized UI response, not a 429 mock', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(401)).toBeVisible()
    await expect(app.errorLab.triggerButton(429)).toBeVisible()
    const response = await liveErrorLabTrigger(page, 401, platformGetStatuses)
    await expectProblemStatus(response, 401)
    const responsePromise = page.waitForResponse(
      res => res.url().includes('/api/error-lab/trigger-401') && res.request().method() === 'GET',
      { timeout: 20_000 },
    )
    await app.errorLab.trigger(401)
    expect((await responsePromise).status()).toBe(401)
    await app.errorLab.problem.expectVisible()
  })

  test('Error Lab remaining live statuses without create come from the backend', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    const remaining = [403, 404, 406, 415] as const
    for (const status of remaining) {
      await expect(app.errorLab.triggerButton(status)).toBeVisible()
      const response = await liveErrorLabTrigger(page, status, platformGetStatuses)
      await expectProblemStatus(response, status)
    }
  })
})
