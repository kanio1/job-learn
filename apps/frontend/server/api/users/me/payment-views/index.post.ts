import { createPaymentViewRequestSchema } from '~/schemas/payment-view.schema'

export default defineEventHandler(async (event) => {
  const body = await readValidatedBody(event, createPaymentViewRequestSchema.parse)
  return backendApi(event, '/api/users/me/payment-views', {
    method: 'POST',
    body,
  })
})
