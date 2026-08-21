export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  }
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return backendApi(event, `/api/org-tree${suffix}`)
})
