import type { CompositeRole } from '~/utils/rbacMatrix'
import { COMPOSITE_ROLES } from '~/utils/rbacMatrix'

export default defineOAuthOidcEventHandler({
  config: {
    clientId: useRuntimeConfig().oauth.oidc.clientId,
    clientSecret: useRuntimeConfig().oauth.oidc.clientSecret,
    openidConfig: useRuntimeConfig().oauth.oidc.openidConfig,
    scope: ['openid', 'profile', 'email'],
    redirectURL: useRuntimeConfig().oauth.oidc.redirectURL
  },
  async onSuccess(event, { user, tokens }) {
    // Decode access token claims to extract realm_access.roles, tenant_id, merchant_id
    let realmRoles: string[] = []
    let tenantId: string | undefined
    let merchantId: string | undefined

    try {
      const encodedPayload = tokens.access_token.split('.')[1] ?? ''
      const normalizedPayload = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
      const paddedPayload = normalizedPayload.padEnd(
        normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
        '='
      )
      const payload = JSON.parse(
        globalThis.atob(paddedPayload)
      )
      const raw: unknown = payload?.realm_access?.roles
      realmRoles = Array.isArray(raw) ? (raw as string[]) : []
      tenantId = typeof payload?.tenant_id === 'string' ? payload.tenant_id : undefined
      merchantId = typeof payload?.merchant_id === 'string' ? payload.merchant_id : undefined
    }
    catch {
      // Malformed token — proceed without roles (session will reflect no capabilities)
    }

    // Intersect realm roles with the five composite names only.
    // Raw authority roles are building blocks, not session-level role labels.
    const roles: CompositeRole[] = realmRoles.filter(
      (r): r is CompositeRole => (COMPOSITE_ROLES as readonly string[]).includes(r)
    )

    // Capture post-login redirect target captured by the middleware.
    // The middleware stores it as a query param ?redirectTo= on the login page.
    const redirectTo = getCookie(event, 'auth_redirect') ?? '/admin/merchants'
    deleteCookie(event, 'auth_redirect')

    await setUserSession(event, {
      user: {
        username: user.preferred_username ?? user.name ?? user.sub,
        email: user.email ?? undefined,
        roles,
        tenantId,
        merchantId,
      },
      secure: {
        accessToken: tokens.access_token,
      },
      loggedInAt: Date.now(),
    })

    return sendRedirect(event, redirectTo)
  },
  onError(event, error) {
    console.error('Keycloak OIDC login error', error)
    return sendRedirect(event, '/login?error=keycloak')
  },
})
