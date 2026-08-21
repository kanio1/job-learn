/**
 * DT-M360-01 — registry columns / mutations by session.
 *
 * What changes: storageState persona. Not URL.
 * Layer: E2E for hide; REST for 403 (API-040 / RA-M360-040).
 * Seed: contract world. Page objects must not branch on role.
 */

import type { MerchantAccessActor } from './MerchantAccessMatrix'

export type MerchantColumnAccessRow = {
  id: string
  actor: MerchantAccessActor | 'SUPPORT_AGENT'
  list: boolean
  create: boolean
  activate: boolean
  tenantColumn: boolean
  importCsv: boolean
  testId: string
}

export const merchantColumnAccessMatrix: readonly MerchantColumnAccessRow[] = [
  {
    id: 'DT-M360-01-admin',
    actor: 'PLATFORM_ADMIN',
    list: true,
    create: true,
    activate: true,
    tenantColumn: true,
    importCsv: false,
    testId: 'PW-M360-SEC-012',
  },
  {
    id: 'DT-M360-01-tenant',
    actor: 'TENANT_ADMIN',
    list: true,
    create: true,
    activate: true,
    tenantColumn: false,
    importCsv: false,
    testId: 'PW-M360-SEC-013',
  },
  {
    id: 'DT-M360-01-support',
    actor: 'SUPPORT_AGENT',
    list: true,
    create: false,
    activate: false,
    tenantColumn: true,
    importCsv: false,
    testId: 'PW-M360-SEC-011',
  },
  {
    id: 'DT-M360-01-readonly',
    actor: 'READ_ONLY_USER',
    list: true,
    create: false,
    activate: false,
    tenantColumn: false,
    importCsv: false,
    testId: 'PW-M360-SEC-010',
  },
  {
    id: 'DT-M360-01-manager',
    actor: 'MERCHANT_MANAGER',
    list: false,
    create: false,
    activate: false,
    tenantColumn: false,
    importCsv: false,
    testId: 'PW-M360-SEC-014',
  },
]
