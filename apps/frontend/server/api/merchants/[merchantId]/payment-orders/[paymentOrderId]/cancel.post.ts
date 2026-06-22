export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  const body = await readBody(event)
  const reqHeaders = getRequestHeaders(event)

  const ifMatch = reqHeaders['if-match'] || reqHeaders['If-Match']
  const idempotencyKey = reqHeaders['idempotency-key'] || reqHeaders['Idempotency-Key'] || `feat010-${Date.now()}-${Math.random().toString(36).slice(2)}`
  const correlationId = reqHeaders['x-correlation-id'] || reqHeaders['X-Correlation-ID']

  return await backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/cancel`, {
    method: 'POST',
    body,
    forwardIfMatch: ifMatch,
    idempotencyKey,
    correlationId
  })
})
