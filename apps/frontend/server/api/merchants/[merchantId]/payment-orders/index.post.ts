export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const body = await readBody(event)
  const idempotencyKey = getHeader(event, 'idempotency-key')
    || `idem-${Date.now()}-${Math.random().toString(36).substring(2, 10)}`

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

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`,
    'Idempotency-Key': idempotencyKey,
  }

  const correlationId = getHeader(event, 'x-correlation-id')
  if (correlationId) {
    headers['X-Correlation-ID'] = correlationId
  }

  try {
    return await $fetch(`${backendUrl}/api/merchants/${merchantId}/payment-orders`, {
      method: 'POST',
      body,
      headers
    })
  } catch (error: any) {
    const statusCode = error?.statusCode || error?.response?.status
    throw createError({
      statusCode: statusCode || 503,
      statusMessage: error?.data?.message || error?.message || 'Payment order creation failed',
      data: error?.data || { error: 'backend_unavailable' }
    })
  }
})
