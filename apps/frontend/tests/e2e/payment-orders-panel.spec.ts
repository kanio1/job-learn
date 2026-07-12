import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

const merchantId = '11111111-1111-4111-8111-111111111111'
const paymentOrderId = '22222222-2222-4222-8222-222222222222'

test('renders payment summary and list panel', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockSummary(page, {
    totalOrders: 4,
    totalAmountMinor: 10000,
    byCurrency: [
      { currency: 'PLN', orderCount: 2, totalAmountMinor: 3000 },
      { currency: 'EUR', orderCount: 1, totalAmountMinor: 3000 },
      { currency: 'USD', orderCount: 1, totalAmountMinor: 4000 },
    ],
    byStatus: [
      { status: 'CREATED', orderCount: 4, totalAmountMinor: 10000 },
    ],
  })
  await mockList(page, {
    content: [paymentOrder('PAY-PANEL-001', 12500, 'PLN')],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  })

  await page.goto(`/admin/merchants/${merchantId}/payments`)

  await expect(page.getByRole('heading', { name: 'Payment Orders', exact: true })).toBeVisible()
  await expect(page.getByText('Total orders')).toBeVisible()
  await expect(page.getByText('10000', { exact: true })).toBeVisible()
  await expect(page.getByText('PLN').first()).toBeVisible()
  await expect(page.getByText('CREATED').first()).toBeVisible()
  await expect(page.getByText('PAY-PANEL-001')).toBeVisible()
  await expect(page.getByText('12500 PLN')).toBeVisible()
  await expect(page.getByRole('link', { name: 'View payment order PAY-PANEL-001' })).toBeVisible()
})

test('renders empty payment order state', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await mockSummary(page, {
    totalOrders: 0,
    totalAmountMinor: 0,
    byCurrency: [],
    byStatus: [],
  })
  await mockList(page, {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  })

  await page.goto(`/admin/merchants/${merchantId}/payments`)

  await expect(page.getByTestId('payment-orders-empty-state')).toBeVisible()
  await expect(page.getByText('This merchant has no payment orders yet.')).toBeVisible()
  await expect(page.getByText('No currency data.')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Create payment order' })).toBeVisible()
  await expect(page.getByTestId('error-state')).not.toBeVisible()
  await expect(page.getByTestId('loading-state')).not.toBeVisible()
})

test('renders forbidden state without payment data', async ({ page }) => {
  await mockAuthenticatedSession(page)
  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary`, async route => {
    await route.fulfill({
      status: 403,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'forbidden', message: 'Forbidden' }),
    })
  })
  await mockList(page, {
    content: [paymentOrder('PAY-FORBIDDEN-001', 5000, 'EUR')],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  })

  await page.goto(`/admin/merchants/${merchantId}/payments`)

  await expect(page.getByText('You do not have permission to view payment orders')).toBeVisible()
  await expect(page.getByText('PAY-FORBIDDEN-001')).not.toBeVisible()
  await expect(page.getByText('5000 EUR')).not.toBeVisible()
})

async function mockSummary(page, body: unknown) {
  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary`, async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
  })
}

async function mockList(page, body: unknown) {
  await page.route(`**/api/merchants/${merchantId}/payment-orders**`, async route => {
    if (route.request().url().includes('/summary')) {
      await route.fallback()
      return
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
  })
}

function paymentOrder(clientOrderReference: string, amountMinor: number, currency: 'PLN' | 'EUR' | 'USD') {
  return {
    paymentOrderId,
    merchantId,
    clientOrderReference,
    amountMinor,
    currency,
    status: 'CREATED',
    createdAt: '2026-05-31T10:00:00Z',
    updatedAt: '2026-05-31T10:00:00Z',
  }
}
