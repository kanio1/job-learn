/**
 * Error Lab — 401 Unauthorized trigger.
 *
 * GETs a protected endpoint WITHOUT any Authorization header.
 * The backend security layer returns 401 because no token is present.
 *
 * Security: intentionally omits the token — that is the point of this scenario.
 * Requirements: 6.1, 6.5
 */
import { forwardLabBackendError } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig()
  const backendUrl = (config.public.apiBaseUrl as string) || 'http://localhost:8080'

  // Deliberately NO Authorization header — triggers 401
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  try {
    const response = await $fetch.raw(`${backendUrl}/api/merchants`, {
      method: 'GET',
      headers,
    })
    for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type', 'WWW-Authenticate']) {
      const val = response.headers.get(name)
      if (val) setHeader(event, name, val)
    }
    return response._data
  } catch (error: any) {
    return forwardLabBackendError(event, error)
  }
})
