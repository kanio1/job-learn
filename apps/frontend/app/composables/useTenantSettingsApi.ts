/**
 * Domain composable for tenant settings (F-C4).
 *
 * GET  /api/tenants/current/settings → settings + ETag
 * PATCH /api/tenants/current/settings → updated settings + new ETag (requires If-Match)
 *
 * Token safety: delegates transport to useApiClient → server/api BFF.
 * Bearer is attached server-side only; never visible to browser JS.
 */

import type { ApiResponse } from '~/types/api'
import { tenantSettingsSchema, type TenantSettings } from '~/schemas/tenant-settings.schema'

export type { TenantSettings }

export function useTenantSettingsApi() {
  const { request } = useApiClient()

  async function getSettings(): Promise<ApiResponse<TenantSettings>> {
    return request('/api/tenants/current/settings', tenantSettingsSchema)
  }

  async function updateSettings(
    settings: TenantSettings,
    ifMatch: string
  ): Promise<ApiResponse<TenantSettings>> {
    return request('/api/tenants/current/settings', tenantSettingsSchema, {
      method: 'PATCH',
      body: settings,
      headers: { 'If-Match': ifMatch },
    })
  }

  return { getSettings, updateSettings }
}
