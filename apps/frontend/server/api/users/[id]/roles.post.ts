import { roleAssignmentSchema } from '~/schemas/user.schema'

export default defineEventHandler(async (event): Promise<unknown> => {
  const id = getRouterParam(event, 'id')
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'User id is required' })
  }

  const body = await readValidatedBody(event, roleAssignmentSchema.parse)
  return backendApi(event, `/api/users/${encodeURIComponent(id)}/roles`, {
    method: 'POST',
    body,
    correlationId: getHeader(event, 'x-correlation-id'),
  })
})
