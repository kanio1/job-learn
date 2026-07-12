import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

/**
 * F-C5 — Sequential route mock retry demo (503 Service Unavailable → 200).
 *
 * Demonstrates stateful route.fulfill() sequencing and the retry idempotency
 * invariant: a GET is safe to retry — every attempt is the same method/URL
 * with no side effects, so retrying never produces a different outcome than
 * a single successful call would have. Reuses the existing ErrorState/Retry
 * UI (no frontend code change needed), the same pattern already proven for
 * a generic 500 in merchant-feedback.spec.ts, but with the semantically
 * correct 503 status and explicit response-sequence assertions.
 *
 * The mock toggles on an explicit `succeeding` flag rather than a fixed
 * attempt count: this page's initial mount can legitimately issue more than
 * one GET /api/merchants before settling (SSR + client hydration), so
 * counting exact attempt indices against user actions would be flaky.
 * Toggling on an explicit signal keeps the test deterministic regardless of
 * how many requests the initial load happens to make.
 */

test('recovers from 503 Service Unavailable via manual retry (F-C5)', async ({ page }) => {
  await mockAuthenticatedSession(page)

  let succeeding = false
  let failureCount = 0
  const seenRequests: { method: string; url: string }[] = []

  await page.route('**/api/merchants', async route => {
    seenRequests.push({ method: route.request().method(), url: route.request().url() })

    if (!succeeding) {
      failureCount += 1
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        headers: { 'Retry-After': '1' },
        body: JSON.stringify({ error: 'service_unavailable', message: 'Merchant registry temporarily unavailable' })
      })
      return
    }

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ merchants: [] }) })
  })

  const firstFailure = page.waitForResponse(res => res.url().includes('/api/merchants') && res.request().method() === 'GET')
  await page.goto('/admin/merchants')

  const problemCard = page.getByTestId('problem-details-card')
  const retryButton = page.getByRole('button', { name: 'Retry' })

  const firstResponse = await firstFailure
  expect(firstResponse.status()).toBe(503)
  expect(firstResponse.headers()['retry-after']).toBe('1')
  await expect(problemCard).toBeVisible()
  expect(failureCount).toBeGreaterThanOrEqual(1)

  // Every attempt so far failed — the initial mount may have retried on its
  // own (SSR + hydration), but the UI must still be showing the error state.
  await expect(retryButton).toBeVisible()

  // Arm success and drive one explicit manual retry via the UI.
  succeeding = true
  const success = page.waitForResponse(res => res.url().includes('/api/merchants') && res.request().method() === 'GET')
  await retryButton.click()
  const successResponse = await success
  expect(successResponse.status()).toBe(200)
  await expect(page.getByText('No merchants have been registered yet')).toBeVisible()

  // Idempotency invariant: every attempt (auto or manual) was the same GET
  // against the same URL — retrying never mutated the request, only re-sent it.
  expect(seenRequests.length).toBeGreaterThanOrEqual(2)
  for (const request of seenRequests) {
    expect(request.method).toBe('GET')
    expect(request.url).toBe(seenRequests[0]!.url)
  }
})
