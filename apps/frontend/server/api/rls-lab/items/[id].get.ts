export default defineEventHandler(async (event) => {
  await requireRlsLabSession(event)
  const itemId = getRouterParam(event, 'id')
  return backendApi(event, `/api/rls-lab/items/${itemId}`)
})
