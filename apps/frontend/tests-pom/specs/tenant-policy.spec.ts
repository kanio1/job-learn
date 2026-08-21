import { uniqueToken } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { requestHeader } from '../utils/network'
import type { TenantSettingsBody } from '../api/bff-client'

test.use({ storageState: pomAuthFiles.platformAdmin })

let snapshot: { settings: TenantSettingsBody, etag: string } | undefined

test.beforeEach(async ({ api }) => {
  const client = requireApi(api)
  const get = await client.getTenantSettings()
  expect(get.status, 'PLATFORM_ADMIN must have platform:tenant:settings:read').toBe(200)
  snapshot = {
    settings: get.body,
    etag: get.headers['etag'] || '',
  }
})

test.afterEach(async ({ api }) => {
  if (!snapshot) {
    return
  }
  const client = requireApi(api)
  const fresh = await client.getTenantSettings()
  const etag = fresh.headers['etag'] || snapshot.etag
  await client.updateTenantSettings(
    {
      contactEmail: snapshot.settings.contactEmail ?? undefined,
      timezone: snapshot.settings.timezone,
      webhookBaseUrl: snapshot.settings.webhookBaseUrl ?? undefined,
      paymentPolicy: snapshot.settings.paymentPolicy,
    },
    etag,
  )
})

test('PW-OPS-E2E-180 OFF → InputNumber disabled', async ({ app }) => {
  await app.tenantSettings.goto()
  await app.tenantSettings.expectLoaded()
  await app.tenantSettings.rules.setAutoCapture(false)
  await expect(app.tenantSettings.rules.maxAmount()).toHaveAttribute('aria-disabled', 'true')
  await expect(app.tenantSettings.rules.maxAmountInput()).toBeDisabled()
})

test('PW-OPS-E2E-181 ON → required; submit empty 400/UI error', async ({ app }) => {
  await app.tenantSettings.goto()
  await app.tenantSettings.expectLoaded()
  await app.tenantSettings.rules.setAutoCapture(true)
  await app.tenantSettings.rules.clearMaxAmount()
  await app.tenantSettings.save()
  await expect(app.tenantSettings.rules.maxError()).toBeVisible()
})

test('PW-OPS-E2E-182 slider 0 and 100 saved', async ({ app, page }) => {
  await app.tenantSettings.goto()
  await app.tenantSettings.expectLoaded()
  const slider = app.tenantSettings.rules.riskSlider()
  await slider.focus()
  await slider.press('Home')
  await app.tenantSettings.rules.expectSlider(0)
  const zeroPatch = page.waitForResponse(response =>
    response.url().includes('/api/tenants/current/settings') && response.request().method() === 'PATCH',
  )
  await app.tenantSettings.save()
  expect((await zeroPatch).status()).toBe(200)
  await app.tenantSettings.goto()
  await app.tenantSettings.expectLoaded()
  await app.tenantSettings.rules.expectSlider(0)

  await slider.focus()
  await slider.press('End')
  await app.tenantSettings.rules.expectSlider(100)
  const hundredPatch = page.waitForResponse(response =>
    response.url().includes('/api/tenants/current/settings') && response.request().method() === 'PATCH',
  )
  await app.tenantSettings.save()
  expect((await hundredPatch).status()).toBe(200)
  await app.tenantSettings.goto()
  await app.tenantSettings.expectLoaded()
  await app.tenantSettings.rules.expectSlider(100)
})

test('PW-OPS-E2E-183 Home → 0; End → 100', async ({ app }) => {
  await app.tenantSettings.goto()
  await app.tenantSettings.expectLoaded()
  const slider = app.tenantSettings.rules.riskSlider()
  await slider.focus()
  await slider.press('End')
  await app.tenantSettings.rules.expectSlider(100)
  await slider.press('Home')
  await app.tenantSettings.rules.expectSlider(0)
  await slider.press('ArrowRight')
  await app.tenantSettings.rules.expectSlider(1)
  await slider.press('ArrowLeft')
  await app.tenantSettings.rules.expectSlider(0)
})

test('PW-OPS-E2E-184 RadioGroup Manual vs Automatic keyboard', async ({ app, page }) => {
  await app.tenantSettings.goto()
  await app.tenantSettings.expectLoaded()
  const manual = app.tenantSettings.rules.refundRadio('Manual')
  const automatic = app.tenantSettings.rules.refundRadio('Automatic')
  await manual.click()
  await expect(manual).toHaveAttribute('aria-checked', 'true')
  await manual.focus()
  // Reka checks the newly focused radio on keydown; keep the key down so
  // the window keyup does not clear that flag before the click runs.
  await page.keyboard.down('ArrowRight')
  await expect(automatic).toHaveAttribute('aria-checked', 'true')
  await page.keyboard.up('ArrowRight')
  await automatic.focus()
  await page.keyboard.down('ArrowLeft')
  await expect(manual).toHaveAttribute('aria-checked', 'true')
  await page.keyboard.up('ArrowLeft')

  const patch = page.waitForRequest(request =>
    request.url().includes('/api/tenants/current/settings') && request.method() === 'PATCH',
  )
  await app.tenantSettings.save()
  expect(requestHeader(await patch, 'If-Match')).toMatch(/^"v\d+"$/)
})

test('rule configurator save still forwards If-Match', async ({ app, page }) => {
  const getPromise = page.waitForResponse(response =>
    response.url().includes('/api/tenants/current/settings') && response.request().method() === 'GET',
  )
  await app.tenantSettings.goto()
  const get = await getPromise
  const etag = get.headers()['etag']
  expect(etag).toBeTruthy()
  await app.tenantSettings.expectLoaded()
  await app.tenantSettings.fillContactEmail(`policy-${uniqueToken().toLowerCase()}@example.com`)
  const patchRequest = page.waitForRequest(request =>
    request.url().includes('/api/tenants/current/settings') && request.method() === 'PATCH',
  )
  await app.tenantSettings.save()
  expect(requestHeader(await patchRequest, 'If-Match')).toBe(etag)
})
