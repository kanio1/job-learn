/**
 * Error Lab — 403 Forbidden.
 * Uses the real session token and an operation the caller is not allowed to perform
 * (create payment order). Invalid JWT is 401, not 403.
 * Fail-closed: 2xx on create means this actor can create → 503 lab_unavailable.
 */
import { forwardLabBackendError, labUnavailableBody, labBackendUrl, sessionAccessToken } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const backendUrl = labBackendUrl()
  const session = await getUserSession(event)
  const accessToken = sessionAccessToken(session)
  const merchantId = '00000000-0000-0000-0000-0000000000b1'

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Idempotency-Key': `error-lab-403-${Date.now()}`,
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  try {
    await $fetch.raw(
      `${backendUrl}/api/merchants/${merchantId}/payment-orders`,
      {
        method: 'POST',
        headers,
        body: {
          amountMinor: 500,
          currency: 'PLN',
          clientOrderReference: `error-lab-403-${Date.now()}`,
        },
      },
    )
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return labUnavailableBody(
      'This session can create payment orders; 403 requires an actor without merchant:payments:create.',
    )
  }
  catch (error: any) {
    return forwardLabBackendError(event, error)
  }
})
