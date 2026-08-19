/**
 * Error Lab — 304 Not Modified trigger.
 *
 * Two-step flow:
 * 1. GET payment order → capture ETag
 * 2. GET same payment order with If-None-Match: ETag → backend returns 304
 *
 * Requires: BE-MVP-001 (If-None-Match support) and BFF-MVP-002 (forward If-None-Match).
 * Requirements: 6.1, Error Lab MVP
 */
import { createLabBackendClient, labFirstOrderId, labUnavailableBody, merchantIdForLabTrigger, sessionMerchantId } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const session = await getUserSession(event)
  const { callBackend, authHeaders } = createLabBackendClient(session)

  const merchantsResult = await callBackend('/api/merchants', { headers: authHeaders() })
  const merchantId = merchantIdForLabTrigger(merchantsResult.data, sessionMerchantId(session))

  let paymentOrderId: string | null = null
  const ordersResult = await callBackend(`/api/merchants/${merchantId}/payment-orders`, {
    headers: authHeaders(),
  })
  if (ordersResult.status === 200 && ordersResult.data) {
    paymentOrderId = labFirstOrderId(ordersResult.data) ?? null
  }

  if (!paymentOrderId) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return labUnavailableBody('No payment order is available for the 304 trigger; seed Alpha first.')
  }

  // Step 2: GET to capture ETag
  const firstGet = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
    { headers: authHeaders() }
  )
  const etag = firstGet.headers.get('etag') ?? firstGet.headers.get('ETag')

  if (!etag) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return {
      type: 'https://api.payment-quality.local/problems/lab-unavailable',
      title: 'No ETag received',
      status: 503,
      detail: 'First GET did not return an ETag. Conditional GET cannot proceed.',
      error: 'lab_unavailable',
    }
  }

  // Step 3: GET with If-None-Match → backend returns 304
  const conditionalGet = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
    { headers: authHeaders({ 'If-None-Match': etag }) }
  )

  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Last-Modified']) {
    const val = conditionalGet.headers.get(name)
    if (val) setHeader(event, name, val)
  }

  setResponseStatus(event, conditionalGet.status)
  return conditionalGet.data ?? null
})
