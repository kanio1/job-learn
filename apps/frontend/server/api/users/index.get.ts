const ALLOWED_QUERY_PARAMS = ['tenantId', 'role', 'status', 'search', 'page', 'size'] as const

export default defineEventHandler(async (event): Promise<unknown> => {
  const query = getQuery(event)
  const params = new URLSearchParams()

  for (const key of ALLOWED_QUERY_PARAMS) {
    const value = query[key]
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item !== undefined) params.append(key, String(item))
      }
    } else if (value !== undefined) {
      params.set(key, String(value))
    }
  }

  const suffix = params.size > 0 ? `?${params.toString()}` : ''
  return backendApi(event, `/api/users${suffix}`, {
    correlationId: getHeader(event, 'x-correlation-id'),
  })
})
