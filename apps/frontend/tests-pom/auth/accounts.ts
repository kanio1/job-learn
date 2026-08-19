import { optionalEnv, requiredEnv } from '../utils/env'

export type PomRole = 'PLATFORM_ADMIN' | 'TENANT_ADMIN' | 'MERCHANT_MANAGER' | 'SUPPORT_AGENT' | 'READ_ONLY_USER'

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

export function tenantAdminAccount(): PomAccount {
  return {
    username: optionalEnv('PLAYWRIGHT_TENANT_ADMIN_USERNAME', 'tenant.admin'),
    password: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_PASSWORD'),
    role: 'TENANT_ADMIN',
    tenantId: 'TENANT_ALPHA',
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

/** Seeded empty merchants for Playwright worker isolation (Fala 1). */
export const POM_WORKER_COUNT = 4

export type WorkerMerchant = {
  index: number
  merchantId: string
  merchantReference: string
}

export const workerMerchants: readonly WorkerMerchant[] = [
  { index: 0, merchantId: '00000000-0000-0000-0000-0000000000d0', merchantReference: 'MERCHANT-W0' },
  { index: 1, merchantId: '00000000-0000-0000-0000-0000000000d1', merchantReference: 'MERCHANT-W1' },
  { index: 2, merchantId: '00000000-0000-0000-0000-0000000000d2', merchantReference: 'MERCHANT-W2' },
  { index: 3, merchantId: '00000000-0000-0000-0000-0000000000d3', merchantReference: 'MERCHANT-W3' },
] as const

export function workerMerchant(index: number): WorkerMerchant {
  const world = workerMerchants[index]
  if (!world) {
    throw new Error(`Playwright worker merchant index must be 0..${POM_WORKER_COUNT - 1}, got ${index}`)
  }
  return world
}

/** Manager whose JWT merchant_id is the worker merchant UUID (not Alpha). */
export function merchantManagerAccountForWorker(index: number): PomAccount {
  const world = workerMerchant(index)
  return {
    username: optionalEnv(`PLAYWRIGHT_MERCHANT_MANAGER_W${index}_USERNAME`, `merchant.manager.w${index}`),
    password: optionalEnv(
      `PLAYWRIGHT_MERCHANT_MANAGER_W${index}_PASSWORD`,
      `merchant.manager.w${index}`,
    ),
    role: 'MERCHANT_MANAGER',
    tenantId: 'TENANT_ALPHA',
    merchantId: world.merchantId,
  }
}

export function readOnlyUserAccount(): PomAccount {
  return {
    username: optionalEnv('PLAYWRIGHT_READ_ONLY_USERNAME', 'readonly.user'),
    password: requiredEnv('PLAYWRIGHT_READ_ONLY_PASSWORD'),
    role: 'READ_ONLY_USER',
    tenantId: 'TENANT_ALPHA',
  }
}

export function supportAgentAccount(): PomAccount {
  return {
    username: optionalEnv('PLAYWRIGHT_SUPPORT_AGENT_USERNAME', 'support.agent'),
    password: requiredEnv('PLAYWRIGHT_SUPPORT_AGENT_PASSWORD'),
    role: 'SUPPORT_AGENT',
    tenantId: 'PLATFORM_TENANT',
  }
}

/** Seeded Alpha merchant UUID used by merchant.manager claims. */
export const merchantAlphaId = '00000000-0000-0000-0000-0000000000b1'

export const merchantAlphaTwoId = '00000000-0000-0000-0000-0000000000b2'

export const merchantBetaId = '00000000-0000-0000-0000-0000000000b3'

export const merchantAlphaReference = 'MERCHANT_ALPHA_001'

export const merchantBetaReference = 'MERCHANT_BETA_001'
