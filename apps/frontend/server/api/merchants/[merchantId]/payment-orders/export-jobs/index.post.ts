export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const idempotencyKey = getHeader(event, 'idempotency-key')
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/export-jobs`, {
    method: 'POST',
    body: {},
    idempotencyKey: idempotencyKey || undefined,
  })
})
