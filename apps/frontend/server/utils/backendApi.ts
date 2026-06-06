import type { H3Event } from 'h3'
import { createError, setHeader } from 'h3'

export async function backendApi(
  event: H3Event,
  path: string,
  opts: {
    method?: string
    body?: any
    headers?: Record<string, string>
    forwardIfMatch?: string
    idempotencyKey?: string
    correlationId?: string
  } = {}
) {
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(opts.headers || {})
  }

  const session = await requireUserSession(event)
  const accessToken = session.secure?.accessToken
  if (!accessToken) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Authenticated session is missing a backend access token',
      data: { error: 'missing_access_token' }
    })
  }

  headers.Authorization = `Bearer ${accessToken}`

  if (opts.forwardIfMatch) {
    headers['If-Match'] = opts.forwardIfMatch
  }
  if (opts.idempotencyKey) {
    headers['Idempotency-Key'] = opts.idempotencyKey
  }
  if (opts.correlationId) {
    headers['X-Correlation-ID'] = opts.correlationId
  }

  try {
    const response = await $fetch.raw(`${backendUrl}${path}`, {
      method: (opts.method || 'GET') as any,
      body: opts.body,
      headers
    })
    forwardBackendHeaders(event, response.headers)
    return response._data
  } catch (error: any) {
    const statusCode = error?.statusCode || error?.response?.status
    if (error?.response?.headers) {
      forwardBackendHeaders(event, error.response.headers)
    }

    throw createError({
      statusCode: statusCode || 503,
      statusMessage: error?.data?.message || error?.message || 'Backend request failed',
      data: error?.data || { error: 'backend_unavailable' }
    })
  }
}

function forwardBackendHeaders(event: H3Event, headers: Headers) {
  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Location', 'Accept-Patch', 'Allow']) {
    const value = headers.get(name) || headers.get(name.toLowerCase())
    if (value) {
      setHeader(event, name, value)
    }
  }
}
