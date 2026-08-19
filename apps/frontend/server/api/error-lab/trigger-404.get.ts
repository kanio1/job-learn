/**
 * Error Lab — 404 Not Found trigger.
 *
 * GETs a merchant with a UUID that does not exist.
 * The backend returns 404 with a problem+json body.
 *
 * Requirements: 6.1, 6.5
 */
import { forwardLabBackendError, labBackendUrl, sessionAccessToken } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const backendUrl = labBackendUrl()

  const session = await getUserSession(event)
  const accessToken = sessionAccessToken(session)

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  // UUID that will never exist in the system
  const nonExistentId = '00000000-0000-0000-0000-000000000000'

  try {
    const response = await $fetch.raw(`${backendUrl}/api/merchants/${nonExistentId}`, {
      method: 'GET',
      headers,
    })
    for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type']) {
      const val = response.headers.get(name)
      if (val) setHeader(event, name, val)
    }
    return response._data
  } catch (error: any) {
    return forwardLabBackendError(event, error)
  }
})
