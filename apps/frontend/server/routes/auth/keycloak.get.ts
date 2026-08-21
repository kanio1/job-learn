import type { CompositeRole } from '~/utils/rbacMatrix'
import { COMPOSITE_ROLES } from '~/utils/rbacMatrix'

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const promptLogin = query.prompt === 'login'

  return defineOAuthOidcEventHandler({
    config: {
      clientId: useRuntimeConfig().oauth.oidc.clientId,
      clientSecret: useRuntimeConfig().oauth.oidc.clientSecret,
      openidConfig: useRuntimeConfig().oauth.oidc.openidConfig,
      scope: ['openid', 'profile', 'email'],
      redirectURL: useRuntimeConfig().oauth.oidc.redirectURL,
      params: promptLogin
        ? { authorization_endpoint: { prompt: 'login' } }
        : undefined,
    },
    async onSuccess(event, { user, tokens }) {
      // Decode access token claims to extract realm_access.roles, tenant_id, merchant_id
      let realmRoles: string[] = []
      let tenantId: string | undefined
      let merchantId: string | undefined
      let subject: string | undefined

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
        subject = typeof payload?.sub === 'string' ? payload.sub : undefined
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

      const id = subject ?? (typeof user.sub === 'string' ? user.sub : undefined)

      await setUserSession(event, {
        user: {
          id,
          username: user.preferred_username ?? user.name ?? user.sub,
          email: user.email ?? undefined,
          roles,
          tenantId,
          merchantId,
        },
        // Never persist id_token here. Sealed nuxt-session cookies must stay under
        // ~4 KB; access_token + id_token together overflow that limit, the browser
        // drops the cookie, and live login lands with an empty session (no user).
        // RP logout uses client_id + post_logout_redirect_uri instead of id_token_hint.
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
  })(event)
})
