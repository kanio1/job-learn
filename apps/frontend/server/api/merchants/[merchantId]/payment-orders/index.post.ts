export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId') ?? getRouterParam(event, 'id')
  const body = await readBody(event)
  const idempotencyKey = getHeader(event, 'idempotency-key')
    || `idem-${Date.now()}-${Math.random().toString(36).substring(2, 10)}`
  const correlationId = getHeader(event, 'x-correlation-id')

  return backendApi(event, `/api/merchants/${merchantId}/payment-orders`, {
    method: 'POST',
    body,
    idempotencyKey,
    correlationId: correlationId || undefined,
  })
})
