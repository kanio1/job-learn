export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')

  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const session = await requireUserSession(event)
  const accessToken = session.secure?.accessToken

  if (!accessToken) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Authenticated session is missing a backend access token',
      data: { error: 'missing_access_token' }
    })
  }

  try {
    const res = await $fetch.raw(`${backendUrl}/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    })
    const body = res._data
    const etag = res.headers.get('etag') || res.headers.get('ETag')
    const cacheControl = res.headers.get('cache-control')
    const vary = res.headers.get('vary')
    const correlationId = res.headers.get('x-correlation-id') || res.headers.get('X-Correlation-ID')
    if (etag) setHeader(event, 'ETag', etag)
    if (cacheControl) setHeader(event, 'Cache-Control', cacheControl)
    if (vary) setHeader(event, 'Vary', vary)
    if (correlationId) setHeader(event, 'X-Correlation-ID', correlationId)
    if (etag && body && typeof body === 'object') {
      ;(body as any).versionMarker = etag
    }
    return body
  } catch (error: any) {
    const statusCode = error?.statusCode || error?.response?.status
    throw createError({
      statusCode: statusCode || 503,
      statusMessage: error?.data?.message || error?.message || 'Failed to load payment order',
      data: error?.data || { error: 'backend_unavailable' }
    })
  }
})
