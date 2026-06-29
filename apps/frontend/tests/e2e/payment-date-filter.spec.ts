/**
 * F-C2: Date Range Picker in Payment Filters
 *
 * Playwright/SDET capabilities demonstrated:
 *   - native input[type="date"] interaction through accessible labels
 *   - page.keyboard.press() for keyboard-only date form traversal
 *   - page.waitForResponse() for query parameter assertions
 *   - expect(page).toHaveURL() for filter state URL sync
 */

import { expect, type Page, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'
import { expectNoTokenInBrowserStorage } from '../support/browser-safety-assertions'
import { expectNoAuthorizationInNetworkResponse } from '../support/network-assertions'

const merchantId = '11111111-1111-4111-8111-111111111111'
const listPath = `/api/merchants/${merchantId}/payment-orders`

const allOrders = [
  paymentOrder('22222222-2222-4222-8222-222222222222', 'PAY-DATE-IN', '2026-06-10T10:00:00Z'),
  paymentOrder('33333333-3333-4333-8333-333333333333', 'PAY-DATE-OUT', '2026-05-20T10:00:00Z'),
]

test('date range filter syncs URL and sends list query params', async ({ page }) => {
  await mockPaymentOrdersPage(page)

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await expect(page.getByTestId('payment-orders-table')).toBeVisible({ timeout: 15000 })
  await expect(page.getByText('PAY-DATE-IN')).toBeVisible()

  await page.getByLabel('Created from').fill('2026-06-01')
  await page.getByLabel('Created to').fill('2026-06-30')

  const filteredResponse = page.waitForResponse(response => {
    const url = new URL(response.url())
    return response.request().method() === 'GET'
      && url.pathname === listPath
      && url.searchParams.get('fromDate') === '2026-06-01'
      && url.searchParams.get('toDate') === '2026-06-30'
  })

  await page.getByTestId('payment-filter-apply').click()
  const response = await filteredResponse

  expectNoAuthorizationInNetworkResponse(response)
  await expect(page).toHaveURL(/fromDate=2026-06-01/)
  await expect(page).toHaveURL(/toDate=2026-06-30/)
  await expect(page.getByText('PAY-DATE-IN')).toBeVisible()
  await expect(page.getByText('PAY-DATE-OUT')).toBeHidden()
  await expectNoTokenInBrowserStorage(page)
})

test('created date inputs support keyboard traversal before applying filters', async ({ page }) => {
  await mockPaymentOrdersPage(page)

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await expect(page.getByLabel('Created from')).toBeVisible({ timeout: 15000 })

  const createdFrom = page.getByLabel('Created from')
  const createdTo = page.getByLabel('Created to')

  await createdFrom.focus()
  await createdFrom.fill('2026-06-01')
  await page.keyboard.press('Tab')
  await createdTo.fill('2026-06-30')
  await page.keyboard.press('Tab')

  await expect(createdFrom).toHaveValue('2026-06-01')
  await expect(createdTo).toHaveValue('2026-06-30')

  await page.getByTestId('payment-filter-apply').click()
  await expect(page).toHaveURL(/fromDate=2026-06-01/)
  await expect(page).toHaveURL(/toDate=2026-06-30/)
  await expectNoTokenInBrowserStorage(page)
})

test('clear filters removes date query params and restores the unfiltered list', async ({ page }) => {
  await mockPaymentOrdersPage(page)

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await page.getByLabel('Created from').fill('2026-07-01')
  await page.getByLabel('Created to').fill('2026-07-31')

  await page.getByTestId('payment-filter-apply').click()
  await expect(page).toHaveURL(/fromDate=2026-07-01/)
  await expect(page.getByTestId('payment-orders-empty-state')).toBeVisible()

  await page.getByTestId('payment-filter-clear').click()

  await expect(page).not.toHaveURL(/fromDate=/)
  await expect(page).not.toHaveURL(/toDate=/)
  await expect(page.getByLabel('Created from')).toHaveValue('')
  await expect(page.getByLabel('Created to')).toHaveValue('')
  await expect(page.getByText('PAY-DATE-IN')).toBeVisible()
  await expect(page.getByText('PAY-DATE-OUT')).toBeVisible()
  await expectNoTokenInBrowserStorage(page)
})

async function mockPaymentOrdersPage(page: Page): Promise<void> {
  await mockRoleSession(page, 'PLATFORM_ADMIN')

  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary**`, route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ totalOrders: 2, totalAmountMinor: 15000, byCurrency: [], byStatus: [] }),
    }),
  )

  await page.route(`**/api/merchants/${merchantId}/payment-orders**`, route => {
    const url = new URL(route.request().url())

    if (url.pathname !== listPath) {
      return route.fallback()
    }

    const fromDate = url.searchParams.get('fromDate')
    const toDate = url.searchParams.get('toDate')
    const content = filterOrders(fromDate, toDate)

    return route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        'X-Correlation-ID': 'date-filter-correlation',
        'Cache-Control': 'no-store',
      },
      body: JSON.stringify({
        content,
        page: 0,
        size: 20,
        totalElements: content.length,
        totalPages: content.length > 0 ? 1 : 0,
      }),
    })
  })
}

function filterOrders(fromDate: string | null, toDate: string | null) {
  if (!fromDate && !toDate) {
    return allOrders
  }

  return allOrders.filter(order => {
    const createdDate = order.createdAt.slice(0, 10)
    return (!fromDate || createdDate >= fromDate) && (!toDate || createdDate <= toDate)
  })
}

function paymentOrder(paymentOrderId: string, clientOrderReference: string, createdAt: string) {
  return {
    paymentOrderId,
    merchantId,
    clientOrderReference,
    amountMinor: 5000,
    currency: 'EUR',
    status: 'CREATED',
    createdAt,
    updatedAt: createdAt,
  }
}
