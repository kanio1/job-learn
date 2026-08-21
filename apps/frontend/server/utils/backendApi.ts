import type { H3Event } from 'h3'
import { createError, setHeader, setResponseStatus } from 'h3'

export async function backendApi(
  event: H3Event,
  path: string,
  opts: {
    method?: string
    body?: any
    headers?: Record<string, string>
    forwardIfMatch?: string
    forwardIfNoneMatch?: string
    idempotencyKey?: string
    correlationId?: string
  } = {}
) {
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const method = (opts.method || 'GET').toUpperCase()
  const headers: Record<string, string> = {}
  if (opts.headers) {
    for (const [key, value] of Object.entries(opts.headers)) {
      headers[key] = value
    }
  }
  const isFormData = typeof FormData !== 'undefined' && opts.body instanceof FormData
  if (method !== 'HEAD' && !isFormData && !headers['Content-Type'] && !headers['content-type']) {
    headers['Content-Type'] = 'application/json'
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
  if (opts.forwardIfNoneMatch) {
    headers['If-None-Match'] = opts.forwardIfNoneMatch
  }
  if (opts.idempotencyKey) {
    headers['Idempotency-Key'] = opts.idempotencyKey
  }
  if (opts.correlationId) {
    headers['X-Correlation-ID'] = opts.correlationId
  }

  try {
    const response = await $fetch.raw(`${backendUrl}${path}`, {
      method: method as any,
      body: method === 'HEAD' ? undefined : opts.body,
      headers,
    })
    forwardBackendHeaders(event, response.headers)
    setResponseStatus(event, response.status)
    if (method === 'HEAD') {
      return ''
    }
    return response._data
  } catch (error: any) {
    return writeSpringError(event, error)
  }
}

export function writeSpringError(event: H3Event, error: any) {
  const statusCode = error?.statusCode || error?.response?.status
  if (error?.response?.headers) {
    forwardBackendHeaders(event, error.response.headers)
  }

  if (statusCode === 304) {
    setResponseStatus(event, 304)
    return undefined
  }

  const contentType = error?.response?.headers?.get?.('content-type') ?? ''
  const data = decodeFetchErrorData(error?.data)
  const springJson = (contentType.includes('application/problem+json')
    || contentType.includes('application/json')
    || isJsonObject(data)) && data != null && data !== ''
  if (springJson && data) {
    setResponseStatus(event, statusCode || 503)
    setHeader(event, 'Content-Type', contentType.includes('json') ? contentType : 'application/problem+json')
    return data
  }
  if (statusCode === 401 || statusCode === 403) {
    setResponseStatus(event, statusCode)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return {
      type: statusCode === 401
        ? 'https://api.payment-quality.local/problems/unauthorized'
        : 'https://api.payment-quality.local/problems/forbidden',
      title: statusCode === 401 ? 'Unauthorized' : 'Forbidden',
      status: statusCode,
      detail: typeof data === 'string' ? data : 'Access denied',
      error: statusCode === 401 ? 'unauthorized' : 'forbidden',
    }
  }

  const failedStatus = statusCode || 503
  setResponseStatus(event, failedStatus)
  setHeader(event, 'Content-Type', 'application/problem+json')
  return {
    type: 'https://api.payment-quality.local/problems/backend-unavailable',
    title: 'Backend Unavailable',
    status: failedStatus,
    detail: typeof data === 'string' ? data : 'Backend request failed',
    error: 'backend_unavailable',
  }
}

function decodeFetchErrorData(data: any) {
  if (data instanceof ArrayBuffer || ArrayBuffer.isView(data)) {
    const bytes = data instanceof ArrayBuffer
      ? new Uint8Array(data)
      : new Uint8Array(data.buffer, data.byteOffset, data.byteLength)
    const text = new TextDecoder().decode(bytes)
    return parseJsonObjectText(text) ?? text
  }
  if (typeof data === 'string') {
    return parseJsonObjectText(data) ?? data
  }
  return data
}

function parseJsonObjectText(text: string) {
  const trimmed = text.trim()
  if (!trimmed.startsWith('{')) {
    return undefined
  }
  try {
    // SAFETY: ofetch error bodies are JSON objects or opaque text; parse failure keeps text.
    return JSON.parse(trimmed) as { type?: string, title?: string, status?: number, detail?: string, error?: string }
  }
  catch {
    return undefined
  }
}

function isJsonObject(data: any): boolean {
  return data !== null
    && data !== undefined
    && data.constructor === Object
}

function forwardBackendHeaders(event: H3Event, headers: Headers) {
  for (const name of [
    'ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Location', 'Accept-Patch', 'Allow',
    'Retry-After', 'WWW-Authenticate', 'Idempotency-Replayed', 'Last-Modified',
    'Lab-Signature', 'Lab-Event-Id',
  ]) {
    const value = headers.get(name) || headers.get(name.toLowerCase())
    if (value) {
      setHeader(event, name, value)
    }
  }
}
