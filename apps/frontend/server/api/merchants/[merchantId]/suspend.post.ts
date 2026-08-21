export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId')
  return backendApi(event, `/api/merchants/${merchantId}/suspend`, {
    method: 'POST',
    forwardIfMatch: getHeader(event, 'if-match'),
  })
})
