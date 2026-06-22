/**
 * Error Lab — 406 Not Acceptable trigger.
 *
 * GETs the merchants list with Accept: application/xml.
 * The backend only produces application/json so it returns 406.
 *
 * Requirements: 6.1, 6.5
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig()
  const backendUrl = (config.public.apiBaseUrl as string) || 'http://localhost:8080'

  const session = await getUserSession(event)
  const accessToken = session?.secure?.accessToken as string | undefined

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
