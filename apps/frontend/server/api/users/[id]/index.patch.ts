import { updateUserSchema } from '~/schemas/user.schema'

export default defineEventHandler(async (event): Promise<unknown> => {
  const id = getRouterParam(event, 'id')
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'User id is required' })
  }

  const body = await readValidatedBody(event, updateUserSchema.parse)
  return backendApi(event, `/api/users/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body,
    correlationId: getHeader(event, 'x-correlation-id'),
  })
})
