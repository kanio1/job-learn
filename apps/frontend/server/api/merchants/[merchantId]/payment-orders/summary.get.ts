export default defineEventHandler(async (event): Promise<unknown> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const query = getQuery(event)
  const params = new URLSearchParams()
  const allowedParams = ['currency', 'status', 'fromDate', 'toDate']

  for (const key of allowedParams) {
    const value = query[key]
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item !== undefined) params.append(key, String(item))
      }
    } else if (value !== undefined) {
      params.set(key, String(value))
    }
  }

  const suffix = params.toString() ? `?${params.toString()}` : ''
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/summary${suffix}`)
})
