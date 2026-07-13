export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId') ?? getRouterParam(event, 'id')
  const query = getQuery(event)
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null) {
      params.set(key, String(value))
    }
  }
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders${suffix}`)
})
