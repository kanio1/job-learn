export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')

  return await backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/history`, {
    method: 'GET'
  })
})
