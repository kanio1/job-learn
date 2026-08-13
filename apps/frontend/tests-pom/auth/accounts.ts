import { optionalEnv, requiredEnv } from '../utils/env'

export type PomRole = 'PLATFORM_ADMIN' | 'MERCHANT_MANAGER'

export interface PomAccount {
  username: string
  password: string
  role: PomRole
  tenantId: string
  merchantId?: string
}

export function platformAdminAccount(): PomAccount {
  return {
    username: optionalEnv('PLAYWRIGHT_PLATFORM_ADMIN_USERNAME', 'platform.admin'),
    password: requiredEnv('PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD'),
    role: 'PLATFORM_ADMIN',
    tenantId: 'PLATFORM_TENANT',
  }
}

export function merchantManagerAccount(): PomAccount {
  return {
    username: optionalEnv('PLAYWRIGHT_MERCHANT_MANAGER_USERNAME', 'merchant.manager'),
    password: requiredEnv('PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD'),
    role: 'MERCHANT_MANAGER',
    tenantId: 'TENANT_ALPHA',
    merchantId: 'MERCHANT_ALPHA_001',
  }
}

/** Seeded Alpha merchant UUID used by merchant.manager claims. */
export const merchantAlphaId = '00000000-0000-0000-0000-0000000000b1'

export const merchantBetaId = '00000000-0000-0000-0000-0000000000b3'
