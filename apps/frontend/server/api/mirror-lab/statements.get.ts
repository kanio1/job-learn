export default defineEventHandler(async (event): Promise<any> => {
  await requireMirrorLabSession(event)
  const format = String(getQuery(event).format || 'csv')
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const session = await requireUserSession(event)
  const accessToken = session.secure?.accessToken
  if (!accessToken) {
    throw createError({ statusCode: 401, statusMessage: 'Missing access token' })
  }
  const pdf = format.toLowerCase() === 'pdf'
  const response = await $fetch.raw(`${backendUrl}/api/mirror-lab/statements?format=${format}`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${accessToken}` },
    responseType: pdf ? 'arrayBuffer' : 'text',
  })
  for (const name of ['Content-Type', 'Content-Disposition', 'Cache-Control', 'X-Correlation-ID']) {
    const value = response.headers.get(name) || response.headers.get(name.toLowerCase())
    if (value) {
      setHeader(event, name, value)
    }
  }
  if (pdf) {
    return new Uint8Array(response._data as ArrayBuffer)
  }
  return response._data
})
