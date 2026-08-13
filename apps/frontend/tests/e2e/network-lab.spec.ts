import { expect, test } from '@playwright/test'
import { mockAuthenticatedSession } from './merchant-support'

test.describe('Network Lab mocked interception', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedSession(page)
    await page.goto('/admin/merchants')
    await expect(page.getByTestId('nav-link-mirror-lab')).toBeVisible({ timeout: 15_000 })
    await page.getByTestId('nav-link-mirror-lab').click()
    await page.getByTestId('mirror-lab-open-network').click()
  })

  test('stateful 503 then 200 via page.route', async ({ page }) => {
    let attempts = 0
    await page.route('**/api/network-lab/trigger-503-retry', async (route) => {
      attempts += 1
      if (attempts === 1) {
        await route.fulfill({
          status: 503,
          headers: { 'Retry-After': '1', 'content-type': 'application/json' },
          body: JSON.stringify({ error: 'service_unavailable' }),
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ status: 'ok', attempts }),
      })
    })
    await page.getByTestId('network-lab-trigger-503').click()
    await expect(page.getByTestId('network-lab-result')).toContainText('503')
    await page.getByTestId('network-lab-trigger-503').click()
    await expect(page.getByTestId('network-lab-result')).toContainText('"status":"ok"')
  })

  test('abort and lie via route.fetch', async ({ page }) => {
    await page.route('**/api/network-lab/slow', route => route.abort('timedout'))
    await page.route('**/api/network-lab/lie-fulfillment', async (route) => {
      const response = await route.fetch()
      await route.fulfill({
        response,
        body: JSON.stringify({ status: 'success', lied: true }),
      })
    })
    await page.getByTestId('network-lab-slow').click()
    await expect(page.getByTestId('network-lab-result')).not.toContainText('"status":"ok"')
    await page.getByTestId('network-lab-lie').click()
    await expect(page.getByTestId('network-lab-result')).toContainText('success')
  })

  test('HAR replay without Cookie or Authorization', async ({ page }) => {
    await page.routeFromHAR('tests/e2e/fixtures/network-lab.har', {
      url: '**/api/network-lab/har-replay',
      update: false,
    })
    await page.getByTestId('network-lab-har').click()
    await expect(page.getByTestId('network-lab-result')).toContainText('"source":"har"')
  })
})
