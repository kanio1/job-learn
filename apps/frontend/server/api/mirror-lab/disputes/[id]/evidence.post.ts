export default defineEventHandler(async (event): Promise<any> => {
  requireMirrorLab(event)
  const id = getRouterParam(event, 'id')
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const session = await requireUserSession(event)
  const accessToken = session.secure?.accessToken
  if (!accessToken) {
    throw createError({ statusCode: 401, statusMessage: 'Missing access token' })
  }
  const parts = await readMultipartFormData(event)
  const file = parts?.find(part => part.name === 'file')
  if (!file?.data) {
    throw createError({ statusCode: 400, statusMessage: 'Evidence file is required' })
  }
  const form = new FormData()
  form.append('file', new Blob([file.data], { type: file.type || 'application/octet-stream' }), file.filename || 'evidence')
  try {
    const response = await $fetch.raw(
      `${backendUrl}/api/mirror-lab/disputes/${id}/evidence`,
      {
        method: 'POST',
        body: form,
        headers: { Authorization: `Bearer ${accessToken}` },
      },
    )
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
