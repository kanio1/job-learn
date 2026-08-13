export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const scenario = getHeader(event, 'lab-force-scenario')
  const headers: Record<string, string> = {}
  if (scenario) headers['Lab-Force-Scenario'] = scenario
  return checkoutLabApi(event, '/api/checkout-lab/bookings', {
    method: 'POST',
    body,
    headers,
  })
})
