export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  const body = await readBody(event)
  const headers = getRequestHeaders(event)

  return await backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/authorize`, {
    method: 'POST',
    body
  })
})
