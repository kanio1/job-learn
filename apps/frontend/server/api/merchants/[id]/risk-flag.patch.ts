export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')
  const body = await readBody(event)
  return backendApi(event, `/api/merchants/${id}/risk-flag`, {
    method: 'PATCH',
    body,
  })
})
