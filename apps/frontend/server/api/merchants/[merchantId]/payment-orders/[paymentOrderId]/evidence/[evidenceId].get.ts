import { createError, getRouterParam, setHeader, setResponseStatus } from 'h3'

export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  const evidenceId = getRouterParam(event, 'evidenceId')
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

  try {
    const response = await $fetch.raw<ArrayBuffer>(
      `${backendUrl}/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence/${evidenceId}`,
      {
        method: 'GET',
        headers: { Authorization: `Bearer ${accessToken}` },
        responseType: 'arrayBuffer',
      },
    )
    for (const name of ['Content-Type', 'Content-Disposition', 'Cache-Control', 'Vary', 'X-Correlation-ID']) {
      const value = response.headers.get(name) || response.headers.get(name.toLowerCase())
      if (value) setHeader(event, name, value)
    }
    setResponseStatus(event, response.status)
    return new Uint8Array(response._data as ArrayBuffer)
  }
  catch (error: any) {
    const statusCode = error?.response?.status || error?.statusCode || 503
    throw createError({
      statusCode,
      statusMessage: error?.data?.message || error?.message || 'Evidence download failed',
      data: error?.data || { error: 'evidence_download_failed' },
    })
  }
})
