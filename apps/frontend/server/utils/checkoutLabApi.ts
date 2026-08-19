import type { H3Event } from 'h3'
import { createError, setHeader, setResponseStatus } from 'h3'

type CheckoutLabCall = {
  method?: string
  body?: unknown
  headers?: Record<string, string>
  requireDashboardSession?: boolean
  useLabBearer?: boolean
  followRedirects?: boolean
}

let cachedLabToken: { token: string, expiresAt: number } | null = null

async function obtainLabToken(backendUrl: string, clientId: string, clientSecret: string): Promise<string> {
  if (cachedLabToken && Date.now() < cachedLabToken.expiresAt) {
    return cachedLabToken.token
  }
  const body = new URLSearchParams({
    grant_type: 'client_credentials',
    client_id: clientId,
    client_secret: clientSecret,
  })
  const response = await fetch(`${backendUrl}/api/checkout-lab/oauth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!response.ok) {
    throw createError({ statusCode: 502, statusMessage: 'Checkout lab OAuth token request failed' })
  }
  const payload = await response.json() as { access_token: string, expires_in?: number }
  cachedLabToken = {
    token: payload.access_token,
    expiresAt: Date.now() + Math.max(30, (payload.expires_in ?? 3600) - 30) * 1000,
  }
  return cachedLabToken.token
}

export async function checkoutLabApi(event: H3Event, path: string, opts: CheckoutLabCall = {}) {
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  if (opts.requireDashboardSession !== false) {
    await requireUserSession(event)
  }
  const headers: Record<string, string> = {}
  if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (opts.headers) {
    for (const [key, value] of Object.entries(opts.headers)) {
      headers[key] = value
    }
  }
  if (opts.useLabBearer !== false && opts.requireDashboardSession !== false) {
    headers.Authorization = `Bearer ${await obtainLabToken(
      backendUrl,
      String(config.checkoutLabOAuthClientId),
      String(config.checkoutLabOAuthSecret),
    )}`
  }
  const response = await fetch(`${backendUrl}${path}`, {
    method: opts.method || 'GET',
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    redirect: opts.followRedirects === false ? 'manual' : 'follow',
  })
  forwardCheckoutHeaders(event, response.headers)
  setResponseStatus(event, response.status)
  const contentType = response.headers.get('content-type') || ''
  if (response.status === 204 || response.status === 304) {
    return undefined
  }
  const text = await response.text()
  if (contentType.includes('application/json') || contentType.includes('application/problem+json')) {
    if (contentType.includes('application/problem+json')) {
      setHeader(event, 'Content-Type', contentType)
    }
    try {
      return text ? JSON.parse(text) : undefined
    } catch {
      return text
    }
  }
  return text
}

function forwardCheckoutHeaders(event: H3Event, headers: Headers) {
  for (const name of [
    'ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Location', 'Accept-Patch', 'Allow',
    'Retry-After', 'WWW-Authenticate', 'Idempotency-Replayed', 'Last-Modified',
    'Lab-Signature', 'Lab-Event-Id', 'Lab-Simulate-Token',
  ]) {
    const value = headers.get(name)
    if (value) {
      setHeader(event, name, value)
    }
  }
}
