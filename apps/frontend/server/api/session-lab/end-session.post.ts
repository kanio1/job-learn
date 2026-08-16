export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  const config = useRuntimeConfig()
  const issuer = `${config.public.keycloakUrl}/realms/${config.public.keycloakRealm}`
  const redirectUri = encodeURIComponent(`${config.oauth?.oidc?.redirectURL?.replace('/auth/keycloak', '') || 'http://localhost:3000'}/login`)
  const clientId = encodeURIComponent(config.oauth?.oidc?.clientId || config.public.keycloakClientId || 'payment-quality-dashboard')
  // Logout without id_token_hint — the BFF session cookie does not store id_token
  // (browser 4 KB cookie limit). Keycloak accepts client_id + post_logout_redirect_uri.
  const endSessionUrl = `${issuer}/protocol/openid-connect/logout?client_id=${clientId}&post_logout_redirect_uri=${redirectUri}`
  await clearUserSession(event)
  return { ended: true, endSessionUrl }
})
