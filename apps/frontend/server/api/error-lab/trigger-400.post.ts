/**
 * Error Lab — 400 Bad Request trigger.
 *
 * POSTs a payment order with an invalid body (negative amountMinor).
 * The backend validation layer rejects it and returns 400 problem+json.
 *
 * Security: bearer token attached server-side, NEVER forwarded to the browser.
 * Requirements: 6.1, 6.5
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig()
  const backendUrl = (config.public.apiBaseUrl as string) || 'http://localhost:8080'

  const session = await getUserSession(event)
  const accessToken = session?.secure?.accessToken as string | undefined

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Idempotency-Key': `error-lab-400-${Date.now()}`,
  }
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  try {
    const response = await $fetch.raw(
      `${backendUrl}/api/merchants/error-lab-merchant/payment-orders`,
      {
        method: 'POST',
        headers,
        body: {
          amountMinor: -1,
          currency: 'PLN',
          clientOrderReference: 'error-lab-invalid',
        },
      }
    )
    for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Location', 'Content-Type']) {
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
