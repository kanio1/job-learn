/**
 * UC — create merchant after M5 (GAP-W2-01 closed).
 *
 * What changes: who is signed in. Platform sees tenant field; tenant.admin uses JWT.
 * Layer: E2E (form + table). Duplicate/length stay on REST partitions.
 * Seed: unique factory. Never seed-learning.
 */

export const createMerchantJourney = {
  platformWithTenantField: {
    id: 'SCN-MER-13',
    actor: 'PLATFORM_ADMIN' as const,
    tenantFieldVisible: true,
    tenantSource: 'form',
  },
  tenantAdminJwt: {
    id: 'SCN-MER-12',
    actor: 'TENANT_ADMIN' as const,
    tenantFieldVisible: false,
    tenantSource: 'jwt',
  },
} as const
