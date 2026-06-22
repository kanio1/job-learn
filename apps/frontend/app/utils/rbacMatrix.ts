/**
 * Single source of truth for the RBAC capability matrix.
 *
 * Each CompositeRole maps to a Capability record that represents the set of
 * fine-grained operations the role grants on the frontend. This constant is
 * consumed by `useAuthorization` and its property test so the matrix is
 * defined once as data (Decision 2 — no drift).
 *
 * Feature: iam-roles-and-keycloak-login
 */

// The five named business Composite Roles modeled in Keycloak.
export type CompositeRole =
  | 'PLATFORM_ADMIN'
  | 'TENANT_ADMIN'
  | 'MERCHANT_MANAGER'
  | 'SUPPORT_AGENT'
  | 'READ_ONLY_USER'

// All five composite role names as a readonly array, useful for iteration and
// exhaustiveness checks (e.g. in property tests).
export const COMPOSITE_ROLES: readonly CompositeRole[] = [
  'PLATFORM_ADMIN',
  'TENANT_ADMIN',
  'MERCHANT_MANAGER',
  'SUPPORT_AGENT',
  'READ_ONLY_USER',
] as const

// Capability booleans derived from the RBAC access matrix.
// Each field mirrors the corresponding Fine_Grained_Authority:
//   canCreateMerchant       → platform:merchants:create
//   canReadMerchants        → platform:merchants:read
//   canUpdateMerchantStatus → platform:merchants:update-status
//   canCreatePaymentOrder   → merchant:payments:create
//   canReadMerchantPayments → merchant:payments:read
//   canReadPlatformPayments → platform:payments:read
//   canRunLifecycle         → merchant:payments:lifecycle | platform:payments:lifecycle
//   canReadAudit            → platform:payments:audit
//   canViewAuditLog         → platform:audit:read | tenant:audit:read
//   canManageUsers          → platform:users:* | tenant:users:*
//   canAssignRoles          → platform:users:assign-roles | tenant:users:assign-roles
export interface Capability {
  canCreateMerchant: boolean
  canReadMerchants: boolean
  canUpdateMerchantStatus: boolean
  canCreatePaymentOrder: boolean
  canReadMerchantPayments: boolean
  canReadPlatformPayments: boolean
  canRunLifecycle: boolean
  canReadAudit: boolean
  canViewAuditLog: boolean
  canManageUsers: boolean
  canAssignRoles: boolean
}

// Convenience constant for a fully-denied capability set (no grants).
const DENY_ALL: Capability = {
  canCreateMerchant: false,
  canReadMerchants: false,
  canUpdateMerchantStatus: false,
  canCreatePaymentOrder: false,
  canReadMerchantPayments: false,
  canReadPlatformPayments: false,
  canRunLifecycle: false,
  canReadAudit: false,
  canViewAuditLog: false,
  canManageUsers: false,
  canAssignRoles: false,
}

/**
 * RBAC matrix mapping each CompositeRole to the capabilities it grants.
 *
 * Derived from the Composite Role Composition table in the design document:
 *
 * | Composite role   | Granted capabilities                                                                        |
 * |------------------|---------------------------------------------------------------------------------------------|
 * | PLATFORM_ADMIN   | canReadMerchants, canCreateMerchant, canUpdateMerchantStatus, canReadPlatformPayments,      |
 * |                  | canRunLifecycle, canReadAudit                                                               |
 * | TENANT_ADMIN     | canReadMerchants, canCreateMerchant, canUpdateMerchantStatus, canReadMerchantPayments       |
 * | MERCHANT_MANAGER | canCreatePaymentOrder, canReadMerchantPayments, canRunLifecycle                             |
 * | SUPPORT_AGENT    | canReadMerchants, canReadPlatformPayments, canReadAudit                                     |
 * | READ_ONLY_USER   | canReadMerchants, canReadPlatformPayments                                                   |
 */
export const rbacMatrix: Record<CompositeRole, Capability> = {
  PLATFORM_ADMIN: {
    ...DENY_ALL,
    canCreateMerchant: true,
    canReadMerchants: true,
    canUpdateMerchantStatus: true,
    canReadPlatformPayments: true,
    canRunLifecycle: true,
    canReadAudit: true,
    canViewAuditLog: true,
    canManageUsers: true,
    canAssignRoles: true,
  },

  TENANT_ADMIN: {
    ...DENY_ALL,
    canCreateMerchant: true,
    canReadMerchants: true,
    canUpdateMerchantStatus: true,
    canReadMerchantPayments: true,
    canViewAuditLog: true,
    canManageUsers: true,
    canAssignRoles: true,
  },

  MERCHANT_MANAGER: {
    ...DENY_ALL,
    canCreatePaymentOrder: true,
    canReadMerchantPayments: true,
    canRunLifecycle: true,
  },

  SUPPORT_AGENT: {
    ...DENY_ALL,
    canReadMerchants: true,
    canReadPlatformPayments: true,
    canReadAudit: true,
    canViewAuditLog: true,
  },

  READ_ONLY_USER: {
    ...DENY_ALL,
    canReadMerchants: true,
    canReadPlatformPayments: true,
  },
}
