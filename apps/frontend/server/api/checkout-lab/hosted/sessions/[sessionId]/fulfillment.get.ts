export default defineEventHandler(async (event) => {
  const sessionId = getRouterParam(event, 'sessionId')
  return checkoutLabApi(event, `/api/checkout-lab/hosted/sessions/${sessionId}/fulfillment`, {
    requireDashboardSession: false,
    useLabBearer: false,
  })
})
