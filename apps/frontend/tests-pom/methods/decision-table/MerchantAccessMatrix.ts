/**
 * DT — who may see / mutate which merchant.
 *
 * What this does: one row = one actor × one resource × one HTTP/UI outcome.
 * What changes between rows: persona (JWT tenant + merchants:read), not the URL.
 * Layer: REST proves authZ codes; E2E proves the UI does not lie.
 * Seed: contract world (~104). Never seed-learning.
 */

export type MerchantAccessActor = 'PLATFORM_ADMIN' | 'TENANT_ADMIN' | 'MERCHANT_MANAGER' | 'READ_ONLY_USER'

export type MerchantAccessRow = {
  id: string
  actor: MerchantAccessActor
  merchantsRead: boolean
  proves: 'platform-scope' | 'tenant-scope' | 'rbac'
  layer: 'e2e' | 'rest' | 'e2e+rest'
  outcome: string
}

export const merchantAccessMatrix = [
  {
    id: 'SCN-ISO-06',
    actor: 'PLATFORM_ADMIN',
    merchantsRead: true,
    proves: 'platform-scope',
    layer: 'e2e',
    outcome: 'table shows MERCHANT_ALPHA_001 and MERCHANT_BETA_001',
  },
  {
    id: 'SCN-ISO-01',
    actor: 'TENANT_ADMIN',
    merchantsRead: true,
    proves: 'tenant-scope',
    layer: 'e2e+rest',
    outcome: 'table shows Alpha; MERCHANT_BETA_001 count 0',
  },
  {
    id: 'SCN-ISO-09',
    actor: 'MERCHANT_MANAGER',
    merchantsRead: false,
    proves: 'rbac',
    layer: 'e2e',
    outcome: 'alert; no table — not tenant isolation',
  },
  {
    id: 'SCN-ISO-03',
    actor: 'TENANT_ADMIN',
    merchantsRead: true,
    proves: 'tenant-scope',
    layer: 'rest',
    outcome: 'GET BETA_001 → 404 problem+json without Beta name',
  },
  {
    id: 'SCN-ISO-02',
    actor: 'TENANT_ADMIN',
    merchantsRead: true,
    proves: 'tenant-scope',
    layer: 'rest',
    outcome: 'GET ALPHA_001 → 200 (proves 404 is not “cannot read”)',
  },
  {
    id: 'SCN-ISO-10',
    actor: 'MERCHANT_MANAGER',
    merchantsRead: false,
    proves: 'rbac',
    layer: 'rest',
    outcome: 'POST payment-order on ALPHA_002 → 403 BOLA',
  },
  {
    id: 'SCN-RO-01',
    actor: 'READ_ONLY_USER',
    merchantsRead: true,
    proves: 'rbac',
    layer: 'e2e',
    outcome: 'registry visible; no create; no lifecycle; no notes form',
  },
] as const satisfies readonly MerchantAccessRow[]
