import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

test('displays payment order detail', async ({ page }) => {
  // Valid RFC 4122 UUIDs: version nibble [1-8], variant nibble [89abAB]
  // Zod 4's z.string().uuid() enforces this strictly.
  const merchantId = '11111111-1111-4111-8111-111111111111'
  const paymentOrderId = '33333333-3333-4333-8333-333333333333'

  // Session mock required: /admin/** is SPA (ssr:false) so page.route intercepts
  // the client-side session fetch triggered by the global auth middleware.
  await mockAuthenticatedSession(page)

  // Mock payment order GET — the BFF proxies to the backend which is not running in CI.
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
        clientOrderReference: 'PAY-READ-001',
        amountMinor: 5000,
        currency: 'EUR',
        status: 'CREATED',
        createdAt: '2026-05-27T10:00:00Z',
        updatedAt: '2026-05-27T10:00:00Z',
      }),
    })
  })

  // Mock history GET — loaded in parallel with getOrder() via Promise.all.
  // Without this mock, store.loadHistory() throws (backend not running), causing
  // Promise.all to reject, which sets pageError and prevents PaymentOrderDetail from rendering.
  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/history`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [] }),
    })
  })

  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [] }),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)

  await expect(page.getByText('Payment Order Detail')).toBeVisible({ timeout: 15000 })
  await expect(page.getByText(paymentOrderId)).toBeVisible()
  // exact:true prevents case-insensitive substring match on "Created At" label
  await expect(page.getByTestId('payment-order-detail').getByText('Created', { exact: true })).toBeVisible()
  await expect(page.getByText('5000 minor units')).toBeVisible()
  await expect(page.getByText('EUR')).toBeVisible()
  await expect(page.getByText('PAY-READ-001')).toBeVisible()
})
