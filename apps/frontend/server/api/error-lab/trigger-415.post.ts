/**
 * Error Lab — 415 Unsupported Media Type trigger.
 *
 * POSTs to an endpoint with Content-Type: text/plain instead of application/json.
 * The backend rejects the content type and returns 415 with problem+json.
 *
 * Requirements: 6.1, 6.5
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig()
  const backendUrl = (config.public.apiBaseUrl as string) || 'http://localhost:8080'

  const session = await getUserSession(event)
  const accessToken = session?.secure?.accessToken as string | undefined

  // Wrong Content-Type: text/plain → 415
  const headers: Record<string, string> = {
    'Content-Type': 'text/plain',
  }
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  try {
    const response = await $fetch.raw(`${backendUrl}/api/merchants`, {
      method: 'POST',
      headers,
      body: 'this is plain text, not json' as any,
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
