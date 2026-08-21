import { createPaymentViewRequestSchema } from '~/schemas/payment-view.schema'

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')
  const body = await readValidatedBody(event, createPaymentViewRequestSchema.parse)
  return backendApi(event, `/api/users/me/payment-views/${id}`, {
    method: 'PUT',
    body,
  })
})
