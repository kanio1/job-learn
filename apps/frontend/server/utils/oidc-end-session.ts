import type { H3Event } from 'h3'

export function oidcEndSessionUrl(event: H3Event): string {
  const config = useRuntimeConfig(event)
  const issuer = `${config.public.keycloakUrl}/realms/${config.public.keycloakRealm}`
  const appOrigin = config.oauth?.oidc?.redirectURL?.replace('/auth/keycloak', '') || 'http://localhost:3000'
  const redirectUri = encodeURIComponent(`${appOrigin}/login`)
  const clientId = encodeURIComponent(config.oauth?.oidc?.clientId || config.public.keycloakClientId || 'payment-quality-dashboard')
  // Logout without id_token_hint — the BFF session cookie does not store id_token
  // (browser 4 KB cookie limit). Keycloak accepts client_id + post_logout_redirect_uri.
  return `${issuer}/protocol/openid-connect/logout?client_id=${clientId}&post_logout_redirect_uri=${redirectUri}`
}

export async function endOidcSession(event: H3Event) {
  const endSessionUrl = oidcEndSessionUrl(event)
  await clearUserSession(event)
  return { ended: true as const, endSessionUrl }
}
