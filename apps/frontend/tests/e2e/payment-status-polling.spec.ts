/**
 * F-B5: Payment Status Polling UI.
 *
 * Playwright capabilities demonstrated:
 *   - page.waitForResponse() for manual refresh network assertion
 *   - response.headers() for repeated GET headers
 *   - expect.poll() for auto-refresh UI state transition
 *   - browser storage token leak guard after repeated GETs
 */

import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'
import { expectNoAuthorizationInNetworkResponse } from '../support/network-assertions'
import { expectNoTokenInBrowserStorage } from '../support/browser-safety-assertions'

const merchantId = '11111111-1111-4111-8111-111111111111'
const paymentOrderId = '33333333-3333-4333-8333-333333333333'
const detailUrl = `**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`
const detailPath = `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`

function paymentOrder(status: 'CREATED' | 'AUTHORIZED' | 'CAPTURED') {
  return {
    paymentOrderId,
    merchantId,
    clientOrderReference: 'PAY-POLL-001',
    amountMinor: 5000,
    currency: 'EUR',
    status,
    createdAt: '2026-06-29T10:00:00Z',
    updatedAt: status === 'CREATED' ? '2026-06-29T10:00:00Z' : '2026-06-29T10:01:00Z',
  }
}

async function mockSupportingPaymentDetailRoutes(page: Parameters<typeof mockAuthenticatedSession>[0]) {
  await page.route(`${detailUrl}/history`, route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [] }),
    }),
  )

  await page.route(`${detailUrl}/evidence`, route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [] }),
    }),
  )
}

test('manual refresh updates status through a repeated GET response', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockSupportingPaymentDetailRoutes(page)

  let detailRequestCount = 0
  await page.route(detailUrl, async (route) => {
    detailRequestCount += 1
    const status = detailRequestCount === 1 ? 'CREATED' : 'AUTHORIZED'
    await route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        ETag: `"v${detailRequestCount}"`,
        'Last-Modified': 'Mon, 29 Jun 2026 10:01:00 GMT',
        'Cache-Control': 'no-store',
        Vary: 'Authorization',
        'X-Correlation-ID': `polling-manual-${detailRequestCount}`,
      },
      body: JSON.stringify(paymentOrder(status)),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)

  await expect(page.getByTestId('payment-status-current')).toHaveText('Created')

  const refreshResponse = page.waitForResponse(response => {
    const responseUrl = new URL(response.url())
    return response.request().method() === 'GET'
      && responseUrl.pathname === detailPath
      && response.status() === 200
  })
  await page.getByTestId('payment-status-refresh').click()
  const response = await refreshResponse

  expect(response.headers()['etag']).toBe('"v2"')
  expect(response.headers()['last-modified']).toBeTruthy()
  expect(response.headers()['x-correlation-id']).toBe('polling-manual-2')
  expectNoAuthorizationInNetworkResponse(response)
  await expect(page.getByTestId('payment-status-current')).toHaveText('Authorized')
  await expect(page.getByTestId('payment-status-last-checked')).not.toContainText('Not checked yet')
  await expectNoTokenInBrowserStorage(page)
})

test('auto refresh updates status using expect.poll without fixed sleeps', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockSupportingPaymentDetailRoutes(page)

  let detailRequestCount = 0
  await page.route(detailUrl, async (route) => {
    detailRequestCount += 1
    const status = detailRequestCount < 3 ? 'AUTHORIZED' : 'CAPTURED'
    await route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        ETag: `"v${detailRequestCount}"`,
        'Cache-Control': 'no-store',
        Vary: 'Authorization',
        'X-Correlation-ID': `polling-auto-${detailRequestCount}`,
      },
      body: JSON.stringify(paymentOrder(status)),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)

  await expect(page.getByTestId('payment-status-current')).toHaveText('Authorized')
  await page.getByTestId('payment-status-auto-refresh').click()

  await expect.poll(
    async () => page.getByTestId('payment-status-current').innerText(),
    {
      message: 'auto refresh should observe the CAPTURED status from a later GET',
      timeout: 5000,
      intervals: [250, 500, 750, 1000],
    },
  ).toBe('Captured')

  expect(detailRequestCount).toBeGreaterThanOrEqual(3)
  await expectNoTokenInBrowserStorage(page)
})
