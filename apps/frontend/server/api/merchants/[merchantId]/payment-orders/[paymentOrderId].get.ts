export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  const ifNoneMatch = getHeader(event, 'If-None-Match')

  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, {
    forwardIfNoneMatch: ifNoneMatch || undefined,
  })
})
