import { uniqueToken } from '../data/factories'
import { test, expect } from '../fixtures'
import { requestHeader } from '../utils/network'
import type { TenantSettingsBody } from '../api/bff-client'

let snapshot: { settings: TenantSettingsBody, etag: string } | undefined

test.beforeEach(async ({ api }) => {
  const get = await api.getTenantSettings()
  expect(get.status).toBe(200)
  snapshot = {
    settings: get.body,
    etag: get.headers['etag'] || '',
  }
})

test.afterEach(async ({ api }) => {
  if (!snapshot) {
    return
  }
  const fresh = await api.getTenantSettings()
  const etag = fresh.headers['etag'] || snapshot.etag
  await api.updateTenantSettings(
    {
      contactEmail: snapshot.settings.contactEmail ?? undefined,
      timezone: snapshot.settings.timezone,
      webhookBaseUrl: snapshot.settings.webhookBaseUrl ?? undefined,
    },
    etag,
  )
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
  await expect(app.page.getByText('Settings saved')).toBeVisible()
})
