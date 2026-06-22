import { createUserSchema } from '~/schemas/user.schema'

export default defineEventHandler(async (event): Promise<unknown> => {
  const body = await readValidatedBody(event, createUserSchema.parse)
  return backendApi(event, '/api/users', {
    method: 'POST',
    body,
    correlationId: getHeader(event, 'x-correlation-id'),
  })
})
