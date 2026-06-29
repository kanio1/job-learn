import { createError, getRequestHeader, getRouterParam, readMultipartFormData, setHeader, setResponseStatus } from 'h3'

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
    })
  }

  const parts = await readMultipartFormData(event)
  const file = parts?.find(part => part.name === 'file')
  if (!file?.data) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Evidence file is required',
      data: { error: 'missing_evidence_file' },
    })
  }

  const form = new FormData()
  const blob = new Blob([file.data], { type: file.type || 'application/octet-stream' })
  form.append('file', blob, file.filename || 'evidence')

  const headers: Record<string, string> = {
    Authorization: `Bearer ${accessToken}`,
  }
  const correlationId = getRequestHeader(event, 'x-correlation-id')
  if (correlationId) {
    headers['X-Correlation-ID'] = correlationId
  }

  try {
    const response = await $fetch.raw(
      `${backendUrl}/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`,
      {
        method: 'POST',
        body: form,
        headers,
      },
    )

    for (const name of ['Content-Type', 'Location', 'Cache-Control', 'Vary', 'X-Correlation-ID']) {
      const value = response.headers.get(name) || response.headers.get(name.toLowerCase())
      if (value) setHeader(event, name, value)
    }

    setResponseStatus(event, response.status)
    return response._data
  }
  catch (error: any) {
    const statusCode = error?.response?.status || error?.statusCode || 503
    const responseHeaders = error?.response?.headers
    if (responseHeaders) {
      for (const name of ['Content-Type', 'Cache-Control', 'Vary', 'X-Correlation-ID']) {
        const value = responseHeaders.get(name) || responseHeaders.get(name.toLowerCase())
        if (value) setHeader(event, name, value)
      }
    }
    throw createError({
      statusCode,
      statusMessage: error?.data?.message || error?.message || 'Evidence upload failed',
      data: error?.data || { error: 'evidence_upload_failed' },
    })
  }
})
