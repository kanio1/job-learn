export default defineEventHandler(async (event) => {
  const sessionId = getRouterParam(event, 'sessionId')
  return checkoutLabApi(event, `/api/checkout-lab/sessions/${sessionId}/fulfillment`)
})
