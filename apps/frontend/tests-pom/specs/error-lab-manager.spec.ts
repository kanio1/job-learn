import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectNoAuthorizationInNetworkResponse } from '../utils/network'

function triggerMethod(status: number): 'GET' | 'POST' {
  return status === 304 ? 'GET' : 'POST'
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

test.describe('Error Lab live triggers (merchant manager)', () => {
  test('Error Lab 400 is a live backend validation problem (not a 429 mock)', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(400)).toBeVisible()
    await expect(app.page.getByTestId('error-lab-trigger-429')).toBeVisible()
    const response = await liveTrigger(page, 400)
    await expectProblemStatus(response, 400)
  })

  test('Error Lab 409 is a live idempotency conflict', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(409)).toBeVisible()
    const response = await liveTrigger(page, 409)
    await expectProblemStatus(response, 409)
  })

  test('Error Lab 412 is a live stale If-Match from the backend', { tag: ['@security'] }, async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(412)).toBeVisible()
    const response = await liveTrigger(page, 412)
    await expectProblemStatus(response, 412)
  })

  test('Error Lab 428 is a live missing If-Match', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(428)).toBeVisible()
    const response = await liveTrigger(page, 428)
    await expectProblemStatus(response, 428)
  })

  test('Error Lab 304 is a live conditional GET', async ({ app, page, api, ownedMerchantId }, testInfo) => {
    const created = await requireApi(api).createPaymentOrder(
      ownedMerchantId,
      {
        amountMinor: 1999,
        currency: 'PLN',
        clientOrderReference: uniqueOrderReference(testInfo, 'E304'),
      },
      uniqueIdempotencyKey(testInfo, 'E304'),
    )
    expect(created.status).toBe(201)

    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(304)).toBeVisible()
    const response = await liveTrigger(page, 304)
    expect(response.status()).toBe(304)
    expectNoAuthorizationInNetworkResponse(response)
  })
})
