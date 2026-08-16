export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const paymentOrderId = getRouterParam(event, 'paymentOrderId')
  const approvalId = getRouterParam(event, 'approvalId')
  const reqHeaders = getRequestHeaders(event)
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-approvals/${approvalId}/approve`, {
    method: 'POST',
    body: {},
    forwardIfMatch: reqHeaders['if-match'] || reqHeaders['If-Match'],
    idempotencyKey: reqHeaders['idempotency-key'] || reqHeaders['Idempotency-Key'],
  })
})
