import type { CompositeRole } from '~/app/utils/rbacMatrix'

declare module '#auth-utils' {
  /**
   * Non-secure session partition — safe to expose to the browser.
   * Never include the bearer token, passwords, or other secrets here.
   */
  interface User {
    username?: string
    email?: string
    /** Intersection of realm_access.roles with the five composite role names. */
    roles?: CompositeRole[]
    /** tenant_id claim value from the JWT (natural-key reference, e.g. TENANT_ALPHA). */
    tenantId?: string
    /** merchant_id claim value from the JWT (natural-key reference, e.g. MERCHANT_ALPHA_001). */
    merchantId?: string
  }

  interface UserSession {
    loggedInAt?: number
  }

  /**
   * Server-only partition — never exposed to the browser.
   * Do not add idToken: the sealed session cookie must stay under ~4 KB.
   */
  interface SecureSessionData {
    accessToken?: string
  }
}

export {}
