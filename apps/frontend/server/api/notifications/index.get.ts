export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const unreadOnly = query.unreadOnly
  const suffix = unreadOnly === undefined || unreadOnly === '' ? '' : `?unreadOnly=${encodeURIComponent(String(unreadOnly))}`
  return backendApi(event, `/api/notifications${suffix}`, {
    method: 'GET',
  })
})
