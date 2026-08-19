/**
 * Error Lab — 406 Not Acceptable trigger.
 *
 * GETs the merchants list with Accept: application/xml.
 * The backend only produces application/json so it returns 406.
 *
 * Requirements: 6.1, 6.5
 */
import { forwardLabBackendError, labBackendUrl, sessionAccessToken } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const backendUrl = labBackendUrl()

  const session = await getUserSession(event)
  const accessToken = sessionAccessToken(session)

  // Accept: application/xml to trigger 406 — backend cannot produce XML
  const headers: Record<string, string> = {
    'Accept': 'application/xml',
  }
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  try {
    const response = await $fetch.raw(`${backendUrl}/api/merchants`, {
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
