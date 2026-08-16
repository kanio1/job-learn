export default defineEventHandler(async (event): Promise<any> => {
  return backendApi(event, '/api/payment-ops/expiration-sweep', { method: 'POST', body: {} })
})
