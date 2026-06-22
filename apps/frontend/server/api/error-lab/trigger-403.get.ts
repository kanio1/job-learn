/**
 * Error Lab — 403 Forbidden trigger.
 *
 * Calls the merchants endpoint with an intentionally invalid/fake token.
 * The backend validates the token and returns 403 because the token lacks
 * the required authority (or is rejected by the security layer).
 *
 * Security: uses a fabricated placeholder token — the real user token is NOT used.
 * Requirements: 6.1, 6.5
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig()
  const backendUrl = (config.public.apiBaseUrl as string) || 'http://localhost:8080'

  // Intentionally invalid token to provoke 403 from the security layer
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer error-lab-invalid-token-triggers-403',
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
    const statusCode: number = error?.response?.status ?? error?.statusCode ?? 503
    const errorData = error?.response?._data ?? error?.data
    if (error?.response?.headers) {
      for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type']) {
        const val = error.response.headers.get(name)
        if (val) setHeader(event, name, val)
      }
    }
    setResponseStatus(event, statusCode)
    return errorData
  }
})
