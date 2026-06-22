export default defineEventHandler(async (event): Promise<unknown> => {
  const id = getRouterParam(event, 'id')
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'User id is required' })
  }

  return backendApi(event, `/api/users/${encodeURIComponent(id)}`, {
    correlationId: getHeader(event, 'x-correlation-id'),
  })
})
