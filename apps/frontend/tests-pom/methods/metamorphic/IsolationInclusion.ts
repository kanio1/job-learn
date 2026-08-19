/**
 * Metamorphic — isolation inclusion (MR-ISO).
 *
 * Every merchant visible to tenant.admin is visible to platform.admin.
 * Beta is not in the tenant.admin set.
 * Layer: REST listMerchants. Complements IsolationDtUc UI rows.
 */

export const mrIso = { id: 'MR-ISO', betaReference: 'MERCHANT_BETA_001' } as const
