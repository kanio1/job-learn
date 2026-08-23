/**
 * useAuthorization — single client-side source of truth for capability-based rendering.
 *
 * Reads the roles array from the server-side session (non-secure partition, safe to expose
 * to the browser) and maps it to capability booleans via the rbacMatrix. Contains no token
 * logic — roles are already resolved server-side by the OIDC onSuccess handler.
 *
 * Feature: iam-roles-and-keycloak-login
 */
import { rbacMatrix, type Capability, type CompositeRole } from '~/utils/rbacMatrix'

export function useAuthorization() {
  const { user } = useUserSession()

  /** The composite roles present in the current session (empty array when not logged in). */
  const roles = computed<CompositeRole[]>(() => {
    const sessionRoles = (user.value as { roles?: unknown })?.roles
    return Array.isArray(sessionRoles) ? (sessionRoles as CompositeRole[]) : []
  })

  /**
   * Merged capability set: a capability is true iff at least one held role grants it.
   * This is a pure function of the current session roles — no backend call, no side effects.
   */
  const can = computed<Capability>(() => {
    const merged: Capability = {
      canCreateMerchant: false,
      canReadMerchants: false,
      canUpdateMerchantStatus: false,
      canUpdateMerchantRiskFlag: false,
      canCreatePaymentOrder: false,
      canReadMerchantPayments: false,
      canReadPlatformPayments: false,
      canRunLifecycle: false,
      canReadAudit: false,
      canViewAuditLog: false,
      canReadPaymentNotes: false,
      canCreatePaymentNote: false,
      canManageTenantSettings: false,
      canManageUsers: false,
      canAssignRoles: false,
      canReadSupport: false,
      canOperateSupport: false,
      canReadOpsFeed: false,
      canInjectOps: false,
      canReadNotifications: false,
      canReadEventLab: false,
      canOperateEventLab: false,
    }
    for (const role of roles.value) {
      const cap = rbacMatrix[role]
      if (!cap) continue
      if (cap.canCreateMerchant) merged.canCreateMerchant = true
      if (cap.canReadMerchants) merged.canReadMerchants = true
      if (cap.canUpdateMerchantStatus) merged.canUpdateMerchantStatus = true
      if (cap.canUpdateMerchantRiskFlag) merged.canUpdateMerchantRiskFlag = true
      if (cap.canCreatePaymentOrder) merged.canCreatePaymentOrder = true
      if (cap.canReadMerchantPayments) merged.canReadMerchantPayments = true
      if (cap.canReadPlatformPayments) merged.canReadPlatformPayments = true
      if (cap.canRunLifecycle) merged.canRunLifecycle = true
      if (cap.canReadAudit) merged.canReadAudit = true
      if (cap.canViewAuditLog) merged.canViewAuditLog = true
      if (cap.canReadPaymentNotes) merged.canReadPaymentNotes = true
      if (cap.canCreatePaymentNote) merged.canCreatePaymentNote = true
      if (cap.canManageTenantSettings) merged.canManageTenantSettings = true
      if (cap.canManageUsers) merged.canManageUsers = true
      if (cap.canAssignRoles) merged.canAssignRoles = true
      if (cap.canReadSupport) merged.canReadSupport = true
      if (cap.canOperateSupport) merged.canOperateSupport = true
      if (cap.canReadOpsFeed) merged.canReadOpsFeed = true
      if (cap.canInjectOps) merged.canInjectOps = true
      if (cap.canReadNotifications) merged.canReadNotifications = true
      if (cap.canReadEventLab) merged.canReadEventLab = true
      if (cap.canOperateEventLab) merged.canOperateEventLab = true
    }
    return merged
  })

  /** Returns true iff the current session includes the given composite role. */
  function hasRole(role: CompositeRole): boolean {
    return roles.value.includes(role)
  }

  return { roles, can, hasRole }
}
