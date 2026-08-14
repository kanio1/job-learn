import { test, expect } from '../fixtures'
import { expectNoAuthorizationInNetworkResponse } from '../utils/network'

test('Error Lab 400 shows problem+json from the real backend', async ({ app, page }) => {
  await app.errorLab.goto()
  await app.errorLab.expectLoaded()

  const trigger = page.waitForResponse(response =>
    response.url().includes('/api/error-lab/trigger-400'),
  )
  await app.errorLab.trigger(400)
  const response = await trigger
  expect(response.status()).toBe(400)
  expectNoAuthorizationInNetworkResponse(response)
  await app.errorLab.problem.expectVisible()
  await app.errorLab.problem.expectStatusBadge(400)
})

test('Error Lab 401 is a live unauthorized response, not a 429 mock', async ({ app, page }) => {
  await app.errorLab.goto()
  await app.errorLab.expectLoaded()
  await expect(app.page.getByTestId('error-lab-trigger-429')).toBeVisible()

  const trigger = page.waitForResponse(response =>
    response.url().includes('/api/error-lab/trigger-401'),
  )
  await app.errorLab.trigger(401)
  const response = await trigger
  expect(response.status()).toBe(401)
  expectNoAuthorizationInNetworkResponse(response)
})

test('Error Lab 412 is a live stale If-Match from the backend', { tag: ['@security'] }, async ({ app, page }) => {
  await app.errorLab.goto()
  await app.errorLab.expectLoaded()
  await expect(app.errorLab.triggerButton(412)).toBeVisible()

  const trigger = page.waitForResponse(response =>
    response.url().includes('/api/error-lab/trigger-412'),
  )
  await app.errorLab.trigger(412)
  const response = await trigger
  expect(response.status()).toBe(412)
  expectNoAuthorizationInNetworkResponse(response)
  await app.errorLab.problem.expectVisible()
  await app.errorLab.problem.expectStatusBadge(412)
})
