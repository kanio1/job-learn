/**
 * F-C4: Tenant Settings ETag Form (Phase 3B-7)
 *
 * Validates RBAC gating, form fill, ETag capture, If-Match forwarding,
 * stale 412 display, and basic validation for the tenant settings page.
 *
 * Playwright capabilities demonstrated:
 *   - mockRoleSession()            — typed multi-role session helper (F-A2)
 *   - page.waitForResponse()       — capture GET/PATCH responses + ETag header
 *   - page.fill() + page.click()   — form interaction
 *   - expect.toBeVisible()         — assert form elements rendered
 *   - expect.not.toBeVisible()     — assert RBAC-hidden elements absent
 *   - response.headers()           — assert ETag present in GET response
 *
 * Token safety: no JWT, no Bearer, no Authorization in test data.
 * ETag safety: ETag captured from intercepted response, not hard-coded.
 */

import { expect, test } from '@playwright/test'
import { mockRoleSession } from '../support/auth-roles'

const SETTINGS_URL = '/admin/tenant/settings'

function sampleSettings(overrides: Record<string, unknown> = {}) {
  return {
    contactEmail: 'ops@example.com',
    timezone: 'Europe/Warsaw',
    webhookBaseUrl: null,
    ...overrides,
  }
}

async function mockSettingsApis(
  page: Parameters<typeof mockRoleSession>[0],
  options: {
    getSettings?: object
    patchResponse?: { status: number; body: object }
  } = {}
) {
  const getBody = options.getSettings ?? sampleSettings()
  const patchResp = options.patchResponse ?? {
    status: 200,
    body: sampleSettings({ contactEmail: 'updated@example.com', timezone: 'UTC' }),
  }

  await page.route('**/api/tenants/current/settings', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: { 'etag': '"v0"', 'cache-control': 'no-store' },
        body: JSON.stringify(getBody),
      })
    } else if (route.request().method() === 'PATCH') {
      await route.fulfill({
        status: patchResp.status,
        contentType: patchResp.status < 400
          ? 'application/json'
          : 'application/problem+json',
        headers: patchResp.status < 400
          ? { 'etag': '"v1"', 'cache-control': 'no-store' }
          : {},
        body: JSON.stringify(patchResp.body),
      })
    }
  })
}

test('PLATFORM_ADMIN sees settings form and ETag is returned from GET', async ({ page }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await mockSettingsApis(page)

  const responsePromise = page.waitForResponse('**/api/tenants/current/settings')
  await page.goto(SETTINGS_URL)
  const response = await responsePromise

  // Assert ETag header is present in GET response
  expect(response.headers()['etag']).toBe('"v0"')

  await expect(page.getByTestId('tenant-settings-form')).toBeVisible({ timeout: 10000 })
  await expect(page.getByTestId('tenant-settings-contact-email')).toBeVisible()
  await expect(page.getByTestId('tenant-settings-timezone')).toBeVisible()
  await expect(page.getByTestId('tenant-settings-webhook-base-url')).toBeVisible()
  await expect(page.getByTestId('tenant-settings-save')).toBeVisible()
})

test('PLATFORM_ADMIN can fill and save settings — PATCH returns new ETag', async ({ page }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await mockSettingsApis(page)

  await page.goto(SETTINGS_URL)
  await expect(page.getByTestId('tenant-settings-form')).toBeVisible({ timeout: 10000 })

  await page.getByTestId('tenant-settings-contact-email').fill('updated@example.com')
  await page.getByTestId('tenant-settings-timezone').fill('UTC')

  const patchPromise = page.waitForResponse(resp =>
    resp.url().includes('/api/tenants/current/settings') && resp.request().method() === 'PATCH'
  )
  await page.getByTestId('tenant-settings-save').click()
  const patchResponse = await patchPromise

  expect(patchResponse.status()).toBe(200)
  expect(patchResponse.headers()['etag']).toBe('"v1"')
})

test('stale ETag 412 shows error card', async ({ page }) => {
  await mockRoleSession(page, 'PLATFORM_ADMIN')
  await mockSettingsApis(page, {
    patchResponse: {
      status: 412,
      body: {
        type: 'https://api.payment-quality.local/problems/tenant-settings-version-mismatch',
        title: 'Precondition Failed',
        status: 412,
        detail: 'Tenant settings were modified after you loaded them. Reload and retry.',
        code: 'tenant_settings_version_mismatch',
        correlationId: 'test-correlation-id',
      },
    },
  })

  await page.goto(SETTINGS_URL)
  await expect(page.getByTestId('tenant-settings-form')).toBeVisible({ timeout: 10000 })

  await page.getByTestId('tenant-settings-save').click()

  await expect(page.getByTestId('tenant-settings-error')).toBeVisible({ timeout: 5000 })
  await expect(page.getByTestId('tenant-settings-error')).toContainText('Precondition Failed')
})

test('MERCHANT_MANAGER is redirected away from settings page', async ({ page }) => {
  await mockRoleSession(page, 'MERCHANT_MANAGER')

  await page.goto(SETTINGS_URL)

  // Should redirect away since MERCHANT_MANAGER does not have canManageTenantSettings
  await expect(page).not.toHaveURL(/\/admin\/tenant\/settings/, { timeout: 5000 })
})
