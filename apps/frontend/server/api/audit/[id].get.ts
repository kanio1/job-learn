export default defineEventHandler(async (event): Promise<unknown> => {
  const id = getRouterParam(event, 'id')
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'Audit event id is required' })
  }

  return backendApi(event, `/api/audit/${encodeURIComponent(id)}`, {
    correlationId: getHeader(event, 'x-correlation-id'),
  })
})

