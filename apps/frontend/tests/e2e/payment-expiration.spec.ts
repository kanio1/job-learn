/**
 * F-D1 — Payment expiration (authorization window countdown).
 *
 * Playwright capabilities demonstrated:
 *   - page.clock.install() / fastForward() — deterministic time-based UI testing,
 *     no real waiting for a countdown or a backend scheduler tick.
 *   - Conditional content: countdown only renders for AUTHORIZED orders with
 *     an expiresAt.
 *
 * Server-side expiry enforcement (the @Scheduled sweep + lazy capture() check)
 * is backend-only and covered by PaymentExpirationServiceTest /
 * PaymentOrderExpiryTest — page.clock only advances the BROWSER's clock, it
 * cannot fast-forward the real backend JVM, so these tests exercise the
 * frontend countdown display and its own zero-crossing behavior, not an
 * actual server-side status flip.
 */

import { expect, type Page, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

const merchantId = '11111111-1111-4111-8111-111111111111'
const authorizedOrderId = '33333333-3333-4333-8333-333333333331'
const createdOrderId = '33333333-3333-4333-8333-333333333332'
const expiredOrderId = '33333333-3333-4333-8333-333333333333'

test('shows a live countdown for an AUTHORIZED order and flips to expired at zero (F-D1)', async ({ page }) => {
  const now = new Date('2026-06-01T12:00:00.000Z')
  const expiresAt = new Date(now.getTime() + 65_000).toISOString() // 65s from "now"

  // pauseAt (not install) — freezes the clock immediately so real page-load/
  // hydration wall-clock time doesn't silently tick the countdown down
  // before the first assertion runs.
  await page.clock.pauseAt(now)
  await mockPaymentOrderPage(page, authorizedOrderId, {
    status: 'AUTHORIZED',
    authorizedAt: now.toISOString(),
    expiresAt,
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${authorizedOrderId}`)
  await expect(page.getByText('Payment Order Detail')).toBeVisible({ timeout: 15000 })

  const countdown = page.getByTestId('expiration-countdown')
  await expect(countdown).toBeVisible()
  await expect(page.getByTestId('expiration-countdown-remaining')).toHaveText('Expires in 1m 5s')

  // Advance the browser clock past the expiry instant — no real waiting.
  await page.clock.fastForward('01:06')

  await expect(page.getByTestId('expiration-countdown-expired')).toBeVisible()
  await expect(page.getByTestId('expiration-countdown-expired')).toHaveText('Authorization expired')
})

test('does not render a countdown for a CREATED order (no authorization window yet) (F-D1)', async ({ page }) => {
  await mockPaymentOrderPage(page, createdOrderId, {
    status: 'CREATED',
    authorizedAt: null,
    expiresAt: null,
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${createdOrderId}`)
  await expect(page.getByText('Payment Order Detail')).toBeVisible({ timeout: 15000 })

  await expect(page.getByTestId('expiration-countdown')).not.toBeVisible()
})

test('does not render a countdown for an already-EXPIRED order (F-D1)', async ({ page }) => {
  await mockPaymentOrderPage(page, expiredOrderId, {
    status: 'EXPIRED',
    authorizedAt: '2026-05-31T12:00:00.000Z',
    expiresAt: '2026-06-01T12:00:00.000Z',
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${expiredOrderId}`)
  await expect(page.getByText('Payment Order Detail')).toBeVisible({ timeout: 15000 })

  // The historical Expires At field still shows the timestamp, but the live
  // countdown (which only makes sense for a currently-AUTHORIZED order) does not.
  await expect(page.getByTestId('expiration-countdown')).not.toBeVisible()
})

async function mockPaymentOrderPage(
  page: Page,
  paymentOrderId: string,
  fields: { status: string, authorizedAt: string | null, expiresAt: string | null },
): Promise<void> {
  await mockAuthenticatedSession(page)

  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, async (route) => {
    if (route.request().method() === 'HEAD') {
      await route.fulfill({ status: 200 })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        paymentOrderId,
        merchantId,
        clientOrderReference: 'PAY-EXPIRE-001',
        amountMinor: 5000,
        currency: 'EUR',
        status: fields.status,
        authorizedAt: fields.authorizedAt,
        expiresAt: fields.expiresAt,
        createdAt: '2026-05-31T12:00:00.000Z',
        updatedAt: '2026-05-31T12:00:00.000Z',
      }),
    })
  })

  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/history`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content: [] }) })
  })

  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content: [] }) })
  })
}
