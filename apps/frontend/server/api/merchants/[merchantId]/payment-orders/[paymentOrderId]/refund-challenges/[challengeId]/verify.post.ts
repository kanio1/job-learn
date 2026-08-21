export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  const challengeId = getRouterParam(event, 'challengeId')
  return backendApi(
    event,
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-challenges/${challengeId}/verify`,
    {
      method: 'POST',
      body: await readBody(event),
    },
  )
})
