/**
 * Error Lab — Idempotency Replay trigger.
 *
 * Cycle behaviour:
 * - First POST: creates new payment order → 201, Idempotency-Replayed: false
 * - Second POST (same key, same body): replay → 200, Idempotency-Replayed: true
 * - Third POST: starts a new cycle (new key)
 *
 * Requires: BE-MVP-002 (Idempotency-Replayed header), BFF-MVP-001 (forward header).
 * Requirements: 6.1, Error Lab MVP
 */

import { createLabBackendClient, labUnavailableBody, merchantIdForLabTrigger, sessionMerchantId } from '../../utils/errorLabBackend'

let storedIdempotencyKey: string | null = null

export default defineEventHandler(async (event) => {
  const session = await getUserSession(event)
  const { callBackend, authHeaders } = createLabBackendClient(session)

  const merchantsResult = await callBackend('/api/merchants', {
    method: 'GET',
    headers: authHeaders(),
  })
  const merchantId = merchantIdForLabTrigger(merchantsResult.data, sessionMerchantId(session))

  if (!merchantId) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return labUnavailableBody('Idempotency replay trigger requires an active merchant.')
  }

  // Determine idempotency key for this cycle
  // On first call (no stored key) — create new key → expect 201
  // On second call (stored key) — reuse key → expect 200 replay; then reset
  const isReplay = storedIdempotencyKey !== null
  if (!storedIdempotencyKey) {
    storedIdempotencyKey = `error-lab-replay-${crypto.randomUUID()}`
  }
  const currentKey = storedIdempotencyKey

  const result = await callBackend(
    `/api/merchants/${merchantId}/payment-orders`,
    {
      method: 'POST',
      headers: authHeaders({ 'Idempotency-Key': currentKey }),
      body: {
        amountMinor: 1000,
        currency: 'PLN',
        clientOrderReference: `error-lab-replay-order`,
      },
    }
  )

  if (isReplay) {
    // Reset after showing replay so next call starts a new cycle
    storedIdempotencyKey = null
  }

  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Idempotency-Replayed', 'Location']) {
    const val = result.headers.get(name)
    if (val) setHeader(event, name, val)
  }

  setResponseStatus(event, result.status)
  return result.data
})
