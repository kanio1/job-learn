import { createError, getHeader, getQuery, setHeader } from 'h3'

const AUDIT_EXPORT_QUERY_PARAMS = [
  ['actor', 'actor'],
  ['action', 'action'],
  ['targetType', 'target_type'],
  ['from', 'from'],
  ['to', 'to'],
  ['page', 'page'],
  ['size', 'size'],
] as const

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const session = await requireUserSession(event)
  const accessToken = session.secure?.accessToken

  if (!accessToken) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Authenticated session is missing a backend access token',
    })
  }

  const query = getQuery(event)
  const params = new URLSearchParams()

  for (const [frontendName, backendName] of AUDIT_EXPORT_QUERY_PARAMS) {
    const value = query[frontendName]
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item !== undefined) params.append(backendName, String(item))
      }
    } else if (value !== undefined) {
      params.set(backendName, String(value))
    }
  }

  const suffix = params.size > 0 ? `?${params.toString()}` : ''

  try {
    const response = await $fetch.raw<string>(
      `${backendUrl}/api/audit/export.json${suffix}`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: 'application/json',
          ...(getHeader(event, 'x-correlation-id') ? { 'X-Correlation-ID': String(getHeader(event, 'x-correlation-id')) } : {}),
        },
        responseType: 'text',
      },
    )

    for (const name of [
      'Content-Type',
      'Content-Disposition',
      'Content-Length',
      'Cache-Control',
      'X-Correlation-ID',
      'Last-Modified',
    ]) {
      const value = response.headers.get(name) || response.headers.get(name.toLowerCase())
      if (value) setHeader(event, name, value)
    }

    return response._data
  }
  catch (error: any) {
    const statusCode = error?.response?.status || error?.statusCode || 503
    throw createError({
      statusCode,
      statusMessage: error?.data?.message || error?.message || 'Audit export failed',
    })
  }
})
