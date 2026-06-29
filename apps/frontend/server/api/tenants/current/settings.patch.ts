export default defineEventHandler(async (event): Promise<any> => {
  const body = await readBody(event)
  const ifMatch = getRequestHeader(event, 'if-match') ?? getRequestHeader(event, 'If-Match')
  return backendApi(event, '/api/tenants/current/settings', {
    method: 'PATCH',
    body,
    forwardIfMatch: ifMatch,
  })
})
