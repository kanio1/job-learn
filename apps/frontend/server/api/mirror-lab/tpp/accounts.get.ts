export default defineEventHandler(async (event): Promise<any> => {
  requireMirrorLab(event)
  const headerToken = getHeader(event, 'x-lab-consent-token')
  const queryToken = getQuery(event).token
  const token = String(headerToken || queryToken || '')
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  try {
    const response = await $fetch.raw(`${backendUrl}/api/mirror-lab/tpp/accounts`, {
      headers: token ? { 'X-Lab-Consent-Token': token } : {},
    })
    setResponseStatus(event, response.status)
    return response._data
  }
  catch (error: any) {
    throw createError({
      statusCode: error?.statusCode || 503,
      data: error?.data,
    })
  }
})
