export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  const body = await readBody(event)
  const reqHeaders = getRequestHeaders(event)

  const ifMatch = reqHeaders['if-match'] || reqHeaders['If-Match']
  const correlationId = reqHeaders['x-correlation-id'] || reqHeaders['X-Correlation-ID']

  return await backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, {
    method: 'PATCH',
    body,
    headers: {
      'Content-Type': 'application/merge-patch+json'
    },
    forwardIfMatch: ifMatch,
    correlationId
  })
})
