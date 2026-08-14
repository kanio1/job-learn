import { test, expect } from '../fixtures'
import { expectNoAuthorizationInNetworkResponse } from '../utils/network'

function triggerMethod(status: number): 'GET' | 'POST' {
  return status === 401 || status === 403 || status === 404 || status === 406 || status === 304
    ? 'GET'
    : 'POST'
}

async function liveTrigger(
  page: import('@playwright/test').Page,
  status: number,
) {
  return page.request.fetch(`/api/error-lab/trigger-${status}`, {
    method: triggerMethod(status),
  })
}

async function expectProblemStatus(
  response: import('@playwright/test').APIResponse,
  status: number,
) {
  expect(response.status()).toBe(status)
  expect(response.headers()['content-type'] ?? '').toMatch(/application\/problem\+json/)
  const body = await response.json() as { status?: number }
  expect(body.status).toBe(status)
  expectNoAuthorizationInNetworkResponse(response)
}

test.describe('Error Lab live triggers (platform admin)', () => {
  test('Error Lab 401 canary is a live unauthorized UI response, not a 429 mock', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(401)).toBeVisible()
    await expect(app.page.getByTestId('error-lab-trigger-429')).toBeVisible()
    const responsePromise = page.waitForResponse(
      res => res.url().includes('/api/error-lab/trigger-401') && res.request().method() === 'GET',
    )
    await app.errorLab.triggerButton(401).click()
    const response = await responsePromise
    expect(response.status()).toBe(401)
    await app.errorLab.problem.expectVisible()
    expect(response.headers()['content-type'] ?? '').toMatch(/application\/problem\+json/)
    expectNoAuthorizationInNetworkResponse(response)
  })

  test('Error Lab remaining live statuses without create come from the backend', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    const remaining = [403, 404, 406, 415] as const
    for (const status of remaining) {
      await expect(app.errorLab.triggerButton(status)).toBeVisible()
      const response = await liveTrigger(page, status)
      await expectProblemStatus(response, status)
    }
  })
})
