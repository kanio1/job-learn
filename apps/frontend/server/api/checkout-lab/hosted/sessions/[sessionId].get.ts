export default defineEventHandler(async (event) => {
  const sessionId = getRouterParam(event, 'sessionId')
  return checkoutLabApi(event, `/api/checkout-lab/hosted/sessions/${sessionId}`, {
    requireDashboardSession: false,
    useLabBearer: false,
  })
})
