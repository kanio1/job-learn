/**
 * F-B3: Evidence Upload — payment order file upload E2E test.
 *
 * Playwright capabilities demonstrated:
 *   - page.waitForEvent('filechooser')
 *   - fileChooser.setFiles()
 *   - multipart upload UI assertion through a mocked BFF endpoint
 *   - browser storage token leak guard after upload
 */

import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'
import { expectNoTokenInBrowserStorage } from '../support/browser-safety-assertions'

const merchantId = '11111111-1111-4111-8111-111111111111'
const paymentOrderId = '33333333-3333-4333-8333-333333333333'
const evidenceId = '44444444-4444-4444-8444-444444444444'

test('uploads payment order evidence and shows returned metadata', async ({ page }) => {
  await mockAuthenticatedSession(page)

  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        paymentOrderId,
        merchantId,
        clientOrderReference: 'PAY-EVIDENCE-001',
        amountMinor: 5000,
        currency: 'EUR',
        status: 'CREATED',
        createdAt: '2026-06-29T10:00:00Z',
        updatedAt: '2026-06-29T10:00:00Z',
      }),
    })
  })

  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/history`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [] }),
    })
  })

  let uploaded = false
  await page.route(`**/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: uploaded
            ? [{
                evidenceId,
                paymentOrderId,
                originalFilename: 'sample-evidence.txt',
                contentType: 'text/plain',
                sizeBytes: 53,
                uploadedAt: '2026-06-29T10:10:00Z',
              }]
            : [],
        }),
      })
      return
    }

    expect(route.request().method()).toBe('POST')
    const headers = route.request().headers()
    expect(headers.authorization).toBeUndefined()
    uploaded = true
    await route.fulfill({
      status: 201,
      headers: {
        'Content-Type': 'application/json',
        Location: `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence/${evidenceId}`,
        'X-Correlation-ID': 'evidence-upload-correlation',
      },
      body: JSON.stringify({
        evidenceId,
        paymentOrderId,
        originalFilename: 'sample-evidence.txt',
        contentType: 'text/plain',
        sizeBytes: 53,
        uploadedAt: '2026-06-29T10:10:00Z',
      }),
    })
  })

  await page.goto(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)

  await expect(page.getByTestId('evidence-upload-input')).toBeVisible({ timeout: 15000 })

  const fileChooserPromise = page.waitForEvent('filechooser')
  await page.getByTestId('evidence-upload-input').click()
  const fileChooser = await fileChooserPromise
  await fileChooser.setFiles('tests/fixtures/evidence/sample-evidence.txt')

  await page.getByTestId('evidence-upload-submit').click()

  await expect(page.locator('.toast-success')).toContainText('Evidence uploaded')
  await expect(page.getByTestId('evidence-list')).toContainText('sample-evidence.txt')
  await expect(page.getByTestId('evidence-file-name')).toHaveText('sample-evidence.txt')
  await expectNoTokenInBrowserStorage(page)
})
