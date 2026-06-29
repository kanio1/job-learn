/**
 * F-C7: Internal Notes on Payment Orders (Phase 3B-6)
 *
 * Validates RBAC gating and interaction for the internal notes section on the
 * payment order detail page.
 *
 * Playwright capabilities demonstrated:
 *   - mockRoleSession()          — typed multi-role session helper (F-A2)
 *   - fill(textarea)             — enter note text into UTextarea
 *   - listitem assertion         — count/check items in the notes list
 *   - expect.not.toBeVisible()   — assert add-form is hidden for MERCHANT_MANAGER
 *   - page.route() composition   — session mock + API mock per test
 *
 * Token safety: no JWT, no Bearer, no Authorization in test data.
 * Security: note bodies rendered with {{ }} text interpolation only — no v-html/innerHTML.
 */

import { expect, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'

const MERCHANT_ID   = 'aaaaaaaa-1111-4111-8111-111111111111'
const ORDER_ID      = 'bbbbbbbb-1111-4111-8111-111111111111'
const NOTE_ID_1     = 'cccccccc-1111-4111-8111-111111111111'
const NOTE_ID_2     = 'dddddddd-1111-4111-8111-111111111111'

const ORDER_BASE_URL = `/admin/merchants/${MERCHANT_ID}/payments/${ORDER_ID}`

function paymentOrderResponse() {
  return {
    paymentOrderId: ORDER_ID,
    merchantId: MERCHANT_ID,
    clientOrderReference: 'NOTES-TEST-001',
    amountMinor: 2000,
    currency: 'EUR',
    status: 'CREATED',
    createdAt: '2026-06-29T10:00:00Z',
    updatedAt: '2026-06-29T10:00:00Z',
  }
}

function mockPaymentApis(
  page: Parameters<typeof mockRoleSession>[0],
  notes: unknown[] = [],
) {
  return Promise.all([
    page.route(`**/api/merchants/${MERCHANT_ID}/payment-orders/${ORDER_ID}`, async route => {
      if (route.request().method() === 'HEAD') {
        await route.fulfill({ status: 200 })
        return
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(paymentOrderResponse()) })
    }),
    page.route(`**/api/merchants/${MERCHANT_ID}/payment-orders/${ORDER_ID}/history`, async route => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content: [] }) })
    }),
    page.route(`**/api/merchants/${MERCHANT_ID}/payment-orders/${ORDER_ID}/evidence`, async route => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content: [] }) })
    }),
    page.route(`**/api/merchants/${MERCHANT_ID}/payment-orders/${ORDER_ID}/notes`, async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(notes) })
      } else if (route.request().method() === 'POST') {
        const body = JSON.parse(route.request().postData() ?? '{}')
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: NOTE_ID_2,
            body: body.body,
            authorDisplay: 'platform.operator',
            createdAt: '2026-06-29T12:00:00Z',
          }),
        })
      }
    }),
  ])
}

test('PLATFORM_ADMIN sees empty notes state and add form', async ({ page }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await mockPaymentApis(page)
  await page.goto(ORDER_BASE_URL)

  await expect(page.getByTestId('payment-internal-notes')).toBeVisible({ timeout: 15000 })
  await expect(page.getByTestId('payment-note-empty')).toBeVisible()
  await expect(page.getByTestId('payment-note-body')).toBeVisible()
  await expect(page.getByTestId('payment-note-submit')).toBeVisible()
})

test('PLATFORM_ADMIN can add a note and sees it in the list', async ({ page }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await mockPaymentApis(page)
  await page.goto(ORDER_BASE_URL)

  await expect(page.getByTestId('payment-note-body')).toBeVisible({ timeout: 15000 })
  await page.getByTestId('payment-note-body').fill('Suspicious large transaction')
  await page.getByTestId('payment-note-submit').click()

  await expect(page.getByTestId('payment-note-item')).toBeVisible({ timeout: 5000 })
  await expect(page.getByTestId('payment-note-item')).toContainText('Suspicious large transaction')
})

test('SUPPORT_AGENT sees notes section with pre-existing notes', async ({ page }) => {
  const existingNotes = [
    {
      id: NOTE_ID_1,
      body: 'Flagged for manual review',
      authorDisplay: 'support.agent',
      createdAt: '2026-06-29T08:00:00Z',
    },
  ]
  await mockRoleSession(page, 'SUPPORT_AGENT')
  await mockPaymentApis(page, existingNotes)
  await page.goto(ORDER_BASE_URL)

  await expect(page.getByTestId('payment-internal-notes')).toBeVisible({ timeout: 15000 })
  await expect(page.getByTestId('payment-note-item')).toBeVisible()
  await expect(page.getByTestId('payment-note-item')).toContainText('Flagged for manual review')
  await expect(page.getByTestId('payment-note-body')).toBeVisible()
})

test('MERCHANT_MANAGER does not see the internal notes section', async ({ page }) => {
  await mockRoleSession(page, 'MERCHANT_MANAGER')
  await mockPaymentApis(page)
  await page.goto(ORDER_BASE_URL)

  // Wait for the page to load (detail panel should appear, notes should not)
  await expect(page.getByText('NOTES-TEST-001')).toBeVisible({ timeout: 15000 })
  await expect(page.getByTestId('payment-internal-notes')).not.toBeVisible()
  await expect(page.getByTestId('payment-note-body')).not.toBeVisible()
  await expect(page.getByTestId('payment-note-submit')).not.toBeVisible()
})

test('SUPPORT_AGENT can add a note', async ({ page }) => {
  await mockRoleSession(page, 'SUPPORT_AGENT')
  await mockPaymentApis(page)
  await page.goto(ORDER_BASE_URL)

  await expect(page.getByTestId('payment-note-body')).toBeVisible({ timeout: 15000 })
  await page.getByTestId('payment-note-body').fill('Customer confirmed intent')
  await page.getByTestId('payment-note-submit').click()

  await expect(page.getByTestId('payment-note-item')).toBeVisible({ timeout: 5000 })
  await expect(page.getByTestId('payment-note-item')).toContainText('Customer confirmed intent')
})

test('multiple notes appear in the list', async ({ page }) => {
  const existingNotes = [
    {
      id: NOTE_ID_1,
      body: 'First note',
      authorDisplay: 'platform.operator',
      createdAt: '2026-06-29T08:00:00Z',
    },
    {
      id: NOTE_ID_2,
      body: 'Second note',
      authorDisplay: 'support.agent',
      createdAt: '2026-06-29T09:00:00Z',
    },
  ]
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await mockPaymentApis(page, existingNotes)
  await page.goto(ORDER_BASE_URL)

  await expect(page.getByTestId('payment-internal-notes')).toBeVisible({ timeout: 15000 })
  const items = page.getByTestId('payment-note-item')
  await expect(items).toHaveCount(2)
  await expect(items.nth(0)).toContainText('First note')
  await expect(items.nth(1)).toContainText('Second note')
})
