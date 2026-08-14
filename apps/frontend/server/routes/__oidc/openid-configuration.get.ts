/**
 * Rewritten OIDC discovery for compose `--full`.
 * Loopback-only: public Caddy Host must not receive docker DNS URLs.
 * Browser / authorize stay on the public HTTPS issuer (Caddy auth vhost).
 * Token, JWKS, userinfo, and other back-channel endpoints stay on Keycloak HTTP.
 */
const BACK_CHANNEL_KEYS = [
  'token_endpoint',
  'jwks_uri',
  'userinfo_endpoint',
  'end_session_endpoint',
  'revocation_endpoint',
  'introspection_endpoint',
  'pushed_authorization_request_endpoint',
  'backchannel_authentication_endpoint',
] as const

function isLoopbackHost(hostHeader: string | undefined): boolean {
  const host = (hostHeader ?? '').split(',')[0]?.trim().toLowerCase() ?? ''
  const hostname = (host.split(']')[0] ?? '').replace('[', '').split(':')[0] ?? ''
  return hostname === '127.0.0.1' || hostname === 'localhost' || hostname === '::1'
}

function rewriteBackChannelUrl(
  value: unknown,
  publicBase: string,
  publicIssuer: string,
  internalBase: string,
  internalRealm: string,
): unknown {
  if (typeof value !== 'string') {
    return value
  }
  if (value.startsWith(publicIssuer)) {
    return internalRealm + value.slice(publicIssuer.length)
  }
  if (value.startsWith(publicBase)) {
    return internalBase + value.slice(publicBase.length)
  }
  return value
}

export default defineEventHandler(async (event) => {
  if (!isLoopbackHost(getRequestHeader(event, 'host'))) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }

  const config = useRuntimeConfig()
  const realm = (config.public.keycloakRealm as string) || 'payment-quality'
  const publicBase = String(config.public.keycloakUrl || '').replace(/\/$/, '')
  const internalBase = String(config.keycloakInternalBaseUrl || '').replace(/\/$/, '')
  const discoverySource = internalBase
    ? `${internalBase}/realms/${realm}/.well-known/openid-configuration`
    : `${publicBase}/realms/${realm}/.well-known/openid-configuration`

  const document = await $fetch<Record<string, unknown>>(discoverySource)
  setHeader(event, 'Cache-Control', 'no-store')

  if (!internalBase || !publicBase) {
    return document
  }

  const publicIssuer = `${publicBase}/realms/${realm}`
  const internalRealm = `${internalBase}/realms/${realm}`
  const rewritten: Record<string, unknown> = {
    ...document,
    issuer: publicIssuer,
    authorization_endpoint: `${publicIssuer}/protocol/openid-connect/auth`,
  }
  for (const key of BACK_CHANNEL_KEYS) {
    if (key in rewritten) {
      rewritten[key] = rewriteBackChannelUrl(
        rewritten[key],
        publicBase,
        publicIssuer,
        internalBase,
        internalRealm,
      )
    }
  }
  return rewritten
})
