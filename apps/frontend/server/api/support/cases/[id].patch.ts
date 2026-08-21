export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')
  const body = await readBody(event)
  return backendApi(event, `/api/support/cases/${id}`, {
    method: 'PATCH',
    body,
    forwardIfMatch: getHeader(event, 'if-match'),
  })
})
