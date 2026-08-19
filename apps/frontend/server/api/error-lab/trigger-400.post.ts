/**
 * Error Lab — 400 Bad Request.
 * POSTs a payment order with amountMinor -1 against a merchant from the session list.
 */
import { forwardLabBackendError, LAB_ALPHA_MERCHANT_ID, labUnavailableBody, merchantIdForLabTrigger, sessionMerchantId, labBackendUrl, sessionAccessToken } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const backendUrl = labBackendUrl()
  const session = await getUserSession(event)
  const accessToken = sessionAccessToken(session)

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Idempotency-Key': `error-lab-400-${Date.now()}`,
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  let merchantId: string | null = null
  try {
    const list = await $fetch.raw(`${backendUrl}/api/merchants`, {
      method: 'GET',
      headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
    })
    merchantId = merchantIdForLabTrigger(list._data, sessionMerchantId(session))
  }
  catch {
    merchantId = sessionMerchantId(session) ?? LAB_ALPHA_MERCHANT_ID
  }

  if (!merchantId) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return labUnavailableBody('No merchant visible to this session for the 400 trigger.')
  }

  try {
    const response = await $fetch.raw(
      `${backendUrl}/api/merchants/${merchantId}/payment-orders`,
      {
        method: 'POST',
        headers,
        body: {
          amountMinor: -1,
          currency: 'PLN',
          clientOrderReference: 'error-lab-invalid',
        },
      },
    )
    for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Location', 'Content-Type']) {
      const val = response.headers.get(name)
      if (val) {
        setHeader(event, name, val)
      }
    }
    return response._data
  }
  catch (error: any) {
    return forwardLabBackendError(event, error)
  }
})
