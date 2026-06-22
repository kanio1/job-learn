const AUDIT_QUERY_PARAMS = [
  ['actor', 'actor'],
  ['action', 'action'],
  ['targetType', 'target_type'],
  ['from', 'from'],
  ['to', 'to'],
  ['page', 'page'],
  ['size', 'size'],
] as const

export default defineEventHandler(async (event): Promise<unknown> => {
  const query = getQuery(event)
  const params = new URLSearchParams()

  for (const [frontendName, backendName] of AUDIT_QUERY_PARAMS) {
    const value = query[frontendName]
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item !== undefined) params.append(backendName, String(item))
      }
    } else if (value !== undefined) {
      params.set(backendName, String(value))
    }
  }

  const suffix = params.size > 0 ? `?${params.toString()}` : ''
  return backendApi(event, `/api/audit${suffix}`, {
    correlationId: getHeader(event, 'x-correlation-id'),
  })
})

