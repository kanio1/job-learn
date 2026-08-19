/**
 * Error Lab — 409 Conflict (Idempotency).
 * Same Idempotency-Key, different bodies, against a merchant from the session list.
 */
let storedIdempotencyKey: string | null = null
let storedMerchantId: string | null = null

import { createLabBackendClient, labUnavailableBody, merchantIdForLabTrigger, sessionMerchantId } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const session = await getUserSession(event)
  const { callBackend, authHeaders } = createLabBackendClient(session)

  const merchantsResult = await callBackend('/api/merchants', {
    method: 'GET',
    headers: authHeaders(),
  })
  const merchantId = merchantIdForLabTrigger(merchantsResult.data, sessionMerchantId(session))

  if (!storedIdempotencyKey || storedMerchantId !== merchantId) {
    const newKey = `error-lab-409-${Date.now()}`
    const first = await callBackend(`/api/merchants/${merchantId}/payment-orders`, {
      method: 'POST',
      headers: authHeaders({ 'Idempotency-Key': newKey }),
      body: {
        amountMinor: 1000,
        currency: 'PLN',
        clientOrderReference: `error-lab-conflict-a-${Date.now()}`,
      },
    })
    if (first.status !== 201 && first.status !== 200) {
      setResponseStatus(event, 503)
      setHeader(event, 'Content-Type', 'application/problem+json')
      return labUnavailableBody('First idempotent create failed for the 409 trigger.')
    }
    storedIdempotencyKey = newKey
    storedMerchantId = merchantId
  }

  const result = await callBackend(`/api/merchants/${merchantId}/payment-orders`, {
    method: 'POST',
    headers: authHeaders({ 'Idempotency-Key': storedIdempotencyKey }),
    body: {
      amountMinor: 2000,
      currency: 'EUR',
      clientOrderReference: `error-lab-conflict-b-${Date.now()}`,
    },
  })

  if (result.status === 409) {
    storedIdempotencyKey = null
    storedMerchantId = null
  }

  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type']) {
    const val = result.headers.get(name)
    if (val) {
      setHeader(event, name, val)
    }
  }
  setResponseStatus(event, result.status)
  return result.data
})
