/**
 * Payment list filters: amount, page-reset, badge, UPagination.
 * Date-only coverage lives in payment-date-filter.spec.ts.
 */

import { expect, type Page, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'
import { expectNoTokenInBrowserStorage } from '../support/browser-safety-assertions'

const merchantId = '11111111-1111-4111-8111-111111111111'
const listPath = `/api/merchants/${merchantId}/payment-orders`

function paymentOrder(
  paymentOrderId: string,
  clientOrderReference: string,
  amountMinor: number,
  createdAt: string,
  pageHint = 0,
) {
  return {
    paymentOrderId,
    merchantId,
    clientOrderReference,
    amountMinor,
    currency: 'PLN',
    status: 'CREATED',
    createdAt,
    updatedAt: createdAt,
    pageHint,
  }
}

const page0 = [
  paymentOrder('22222222-2222-4222-8222-222222222222', 'PAY-AMT-LOW', 1000, '2026-06-10T10:00:00Z'),
  paymentOrder('33333333-3333-4333-8333-333333333333', 'PAY-AMT-HIGH', 9000, '2026-06-11T10:00:00Z'),
]

test('amount and status filters sync URL query params', async ({ page }) => {
  await mockList(page, { totalPages: 1, totalElements: 2 })

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await expect(page.getByTestId('payment-orders-table')).toBeVisible({ timeout: 15_000 })

  await fillAmount(page, '5000', '10000')
  await page.getByLabel('Status').click()
  await page.getByRole('option', { name: 'Created' }).click()

  const filtered = page.waitForResponse((response) => {
    const url = new URL(response.url())
    return response.request().method() === 'GET'
      && url.pathname === listPath
      && url.searchParams.get('minAmount') === '5000'
      && url.searchParams.get('maxAmount') === '10000'
      && url.searchParams.get('status') === 'CREATED'
  })
  await page.getByTestId('payment-filter-apply').click()
  await filtered

  await expect(page).toHaveURL(/minAmount=5000/)
  await expect(page).toHaveURL(/status=CREATED/)
  await expect(
    page.getByTestId('payment-orders-table')
      .locator('[role="row"], tr')
      .filter({ hasText: 'PAY-AMT-HIGH' })
      .getByTestId('payment-status-badge'),
  ).toHaveAttribute('data-status', 'CREATED')
  await expectNoTokenInBrowserStorage(page)
})

test('applying filters from page 2 resets to page 0', async ({ page }) => {
  await mockList(page, { totalPages: 3, totalElements: 25 })

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await expect(page.getByTestId('payment-orders-pagination')).toBeVisible({ timeout: 15_000 })

  const pageTwo = page.waitForResponse((response) => {
    const url = new URL(response.url())
    return response.request().method() === 'GET'
      && url.pathname === listPath
      && url.searchParams.get('page') === '1'
  })
  await page.getByTestId('payment-orders-pagination').getByRole('button', { name: /Page 2|^2$/ }).click()
  await pageTwo
  await expect(page).toHaveURL(/page=1/)

  await fillAmount(page, '1000', '20000')
  const reset = page.waitForRequest((request) => {
    if (request.method() !== 'GET') {
      return false
    }
    const url = new URL(request.url())
    if (url.pathname !== listPath) {
      return false
    }
    const pageParam = url.searchParams.get('page')
    return pageParam === null || pageParam === '0'
  })
  await page.getByTestId('payment-filter-apply').click()
  await reset
  await expect(page).not.toHaveURL(/page=1/)
})

test('pagination control is 1-based in the widget and 0-based in the query', async ({ page }) => {
  await mockList(page, { totalPages: 3, totalElements: 25 })

  await page.goto(`/admin/merchants/${merchantId}/payments`)
  await expect(page.getByTestId('payment-orders-pagination')).toBeVisible({ timeout: 15_000 })

  const nextPage = page.waitForResponse((response) => {
    const url = new URL(response.url())
    return response.request().method() === 'GET'
      && url.pathname === listPath
      && url.searchParams.get('page') === '1'
  })
  await page.getByTestId('payment-orders-pagination').getByRole('button', { name: /Page 2|^2$/ }).click()
  await nextPage
  await expect(page).toHaveURL(/page=1/)
})

async function fillAmount(page: Page, min: string, max: string) {
  await page.locator('[data-testid="payment-filter-min-amount"] input, input[data-testid="payment-filter-min-amount"]').first().fill(min)
  await page.locator('[data-testid="payment-filter-max-amount"] input, input[data-testid="payment-filter-max-amount"]').first().fill(max)
}

async function mockList(page: Page, meta: { totalPages: number, totalElements: number }) {
  await mockRoleSession(page, 'PLATFORM_ADMIN')

  await page.route(`**/api/merchants/${merchantId}/payment-orders/summary**`, route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ totalOrders: 2, totalAmountMinor: 10000, byCurrency: [], byStatus: [] }),
    }),
  )

  await page.route(`**/api/merchants/${merchantId}/payment-orders**`, route => {
    const url = new URL(route.request().url())
    if (url.pathname !== listPath) {
      return route.fallback()
    }
    const pageIndex = Number(url.searchParams.get('page') || '0')
    const minAmount = url.searchParams.get('minAmount')
    let content = page0
    if (minAmount && Number(minAmount) >= 5000) {
      content = page0.filter(order => order.amountMinor >= 5000)
    }
    if (pageIndex > 0) {
      content = [
        paymentOrder('44444444-4444-4444-8444-444444444444', 'PAY-PAGE-2', 1500, '2026-06-01T10:00:00Z'),
      ]
    }
    return route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        'X-Correlation-ID': 'filter-correlation',
        'Cache-Control': 'no-store',
      },
      body: JSON.stringify({
        content,
        page: pageIndex,
        size: 20,
        totalElements: meta.totalElements,
        totalPages: meta.totalPages,
      }),
    })
  })
}
