export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-challenges`, {
    method: 'POST',
    body: await readBody(event).catch(() => ({})),
  })
})
