export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`)
})
