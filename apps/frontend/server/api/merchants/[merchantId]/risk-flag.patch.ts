export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId')
  const body = await readBody(event)
  return backendApi(event, `/api/merchants/${merchantId}/risk-flag`, {
    method: 'PATCH',
    body,
  })
})
