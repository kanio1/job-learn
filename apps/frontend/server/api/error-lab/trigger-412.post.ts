/**
 * Error Lab — 412 Precondition Failed.
 * Create a real order, then authorize with stale If-Match `"v99"`.
 */
import { createLabBackendClient, labCreatedOrderId, labUnavailableBody, merchantIdForLabTrigger, sessionMerchantId } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const session = await getUserSession(event)
  const { callBackend, authHeaders } = createLabBackendClient(session)

  const merchantsResult = await callBackend('/api/merchants', {
    method: 'GET',
    headers: authHeaders(),
  })
  const merchantId = merchantIdForLabTrigger(merchantsResult.data, sessionMerchantId(session))

  const createResult = await callBackend(
    `/api/merchants/${merchantId}/payment-orders`,
    {
      method: 'POST',
      headers: authHeaders({
        'Idempotency-Key': `error-lab-412-create-${Date.now()}`,
      }),
      body: {
        amountMinor: 500,
        currency: 'PLN',
        clientOrderReference: `error-lab-412-${Date.now()}`,
      },
    },
  )

  const paymentOrderId = labCreatedOrderId(createResult.data)
  if ((createResult.status !== 201 && createResult.status !== 200) || !paymentOrderId) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return labUnavailableBody('Could not create a payment order for the 412 trigger.')
  }

  const result = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/authorize`,
    {
      method: 'POST',
      headers: authHeaders({
        'If-Match': '"v99"',
        'Idempotency-Key': `error-lab-412-auth-${Date.now()}`,
      }),
      body: {},
    },
  )

  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type']) {
    const val = result.headers.get(name)
    if (val) {
      setHeader(event, name, val)
    }
  }
  setResponseStatus(event, result.status)
  return result.data
})
