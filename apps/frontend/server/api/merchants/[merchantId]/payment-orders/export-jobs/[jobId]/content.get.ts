import { createError, getRouterParam, setHeader, setResponseStatus } from 'h3'

export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId')
  const jobId = getRouterParam(event, 'jobId')
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
    const response = await $fetch.raw<string>(
      `${backendUrl}/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}/content`,
      {
        method: 'GET',
        headers: { Authorization: `Bearer ${accessToken}`, Accept: 'text/csv' },
        responseType: 'text',
      },
    )
    for (const name of ['Content-Type', 'Content-Disposition', 'Cache-Control', 'X-Correlation-ID', 'Retry-After']) {
      const value = response.headers.get(name) || response.headers.get(name.toLowerCase())
      if (value) setHeader(event, name, value)
    }
    setResponseStatus(event, response.status)
    return response._data
  }
  catch (error: any) {
    const statusCode = error?.response?.status || error?.statusCode || 503
    throw createError({
      statusCode,
      statusMessage: error?.data?.message || error?.message || 'Export job download failed',
      data: error?.data || { error: 'export_job_not_ready' },
    })
  }
})
