import { uniqueIdempotencyKey, uniqueOrderReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectNoAuthorizationInNetworkResponse } from '../utils/network'
import { expectProblemStatus, liveErrorLabTrigger } from '../utils/error-lab'

const managerGetStatuses = [304] as const

test.describe('Error Lab live triggers (merchant manager)', () => {
  test('Error Lab 400 is a live backend validation problem (not a 429 mock)', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(400)).toBeVisible()
    await expect(app.errorLab.triggerButton(429)).toBeVisible()
    const response = await liveErrorLabTrigger(page, 400, managerGetStatuses)
    await expectProblemStatus(response, 400)
  })

  test('Error Lab 409 is a live idempotency conflict', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(409)).toBeVisible()
    const response = await liveErrorLabTrigger(page, 409, managerGetStatuses)
    await expectProblemStatus(response, 409)
  })

  test('Error Lab 412 is a live stale If-Match from the backend', { tag: ['@security'] }, async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(412)).toBeVisible()
    const response = await liveErrorLabTrigger(page, 412, managerGetStatuses)
    await expectProblemStatus(response, 412)
  })

  test('Error Lab 428 is a live missing If-Match', async ({ app, page }) => {
    await app.errorLab.goto()
    await app.errorLab.expectLoaded()
    await expect(app.errorLab.triggerButton(428)).toBeVisible()
    const response = await liveErrorLabTrigger(page, 428, managerGetStatuses)
    await expectProblemStatus(response, 428)
  })

  test('Error Lab 304 is a live conditional GET', async ({ app, page, api, ownedMerchantId }, testInfo) => {
    const created = await api.payments.createOrder(
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
    const response = await liveErrorLabTrigger(page, 304, managerGetStatuses)
    expect(response.status()).toBe(304)
    expectNoAuthorizationInNetworkResponse(response)
  })
})
