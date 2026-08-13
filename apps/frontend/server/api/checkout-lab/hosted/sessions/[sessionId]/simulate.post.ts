export default defineEventHandler(async (event) => {
  const sessionId = getRouterParam(event, 'sessionId')
  const body = await readBody(event)
  const simulateToken = getHeader(event, 'lab-simulate-token')
  const headers: Record<string, string> = {}
  if (simulateToken) {
    headers['Lab-Simulate-Token'] = simulateToken
  }
  return checkoutLabApi(event, `/api/checkout-lab/hosted/sessions/${sessionId}/simulate`, {
    method: 'POST',
    body,
    headers,
    requireDashboardSession: false,
    useLabBearer: false,
  })
})
