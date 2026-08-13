export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const correlationId = getHeader(event, 'x-correlation-id')
  const scenario = getHeader(event, 'lab-force-scenario')
  const idempotencyKey = getHeader(event, 'idempotency-key')
  const headers: Record<string, string> = {}
  if (correlationId) headers['X-Correlation-ID'] = correlationId
  if (scenario) headers['Lab-Force-Scenario'] = scenario
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey
  return checkoutLabApi(event, '/api/checkout-lab/sessions', {
    method: 'POST',
    body,
    headers,
    followRedirects: false,
  })
})
