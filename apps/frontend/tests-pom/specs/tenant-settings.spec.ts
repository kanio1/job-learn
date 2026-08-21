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

test('PATCH tenant settings without If-Match is 428; stale If-Match is 412', async ({ api }) => {
  const client = requireApi(api)
  const get = await client.getTenantSettings()
  expect(get.status).toBe(200)
  const missing = await client.updateTenantSettings({ timezone: get.body?.timezone })
  expect(missing.status).toBe(428)
  const stale = await client.updateTenantSettings({ timezone: get.body?.timezone }, '"v99"')
  expect(stale.status).toBe(412)
})

test('PATCH tenant settings forwards GET ETag as If-Match', async ({ app, page }) => {
  const getPromise = page.waitForResponse(response =>
    response.url().includes('/api/tenants/current/settings') && response.request().method() === 'GET',
  )
  await app.tenantSettings.goto()
  const get = await getPromise
  const etag = get.headers()['etag']
  expect(etag, 'GET tenant settings must return ETag').toBeTruthy()
  await app.tenantSettings.expectLoaded()

  const email = `pom-${uniqueToken().toLowerCase()}@example.com`
  await app.tenantSettings.fillContactEmail(email)

  const patchRequest = page.waitForRequest(request =>
    request.url().includes('/api/tenants/current/settings') && request.method() === 'PATCH',
  )
  const patchResponse = page.waitForResponse(response =>
    response.url().includes('/api/tenants/current/settings') && response.request().method() === 'PATCH',
  )
  await app.tenantSettings.save()
  const posted = await patchRequest
  const patch = await patchResponse

  expect(requestHeader(posted, 'If-Match')).toBe(etag)
  expect(patch.status()).toBe(200)
  expect(patch.headers()['etag']).toBeTruthy()
  await expect(app.page.getByText('Settings saved', { exact: true })).toBeVisible()
})
