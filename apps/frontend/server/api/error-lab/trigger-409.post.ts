/**
 * Error Lab — 409 Conflict (Idempotency).
 * Same Idempotency-Key, different bodies, against a merchant from the session list.
 */
let storedIdempotencyKey: string | null = null
let storedMerchantId: string | null = null

import { labUnavailableBody, merchantIdForLabTrigger, sessionMerchantId } from '../../utils/errorLabBackend'

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig()
  const backendUrl = (config.public.apiBaseUrl as string) || 'http://localhost:8080'
  const session = await getUserSession(event)
  const accessToken = session?.secure?.accessToken as string | undefined

  function authHeaders(extra: Record<string, string> = {}): Record<string, string> {
    return {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...extra,
    }
  }

  async function callBackend(
    path: string,
    opts: { method?: string, headers?: Record<string, string>, body?: unknown },
  ): Promise<{ status: number, headers: Headers, data: unknown }> {
    try {
      const res = await $fetch.raw(`${backendUrl}${path}`, {
        method: (opts.method ?? 'POST') as 'GET' | 'POST',
        headers: opts.headers,
        body: opts.body as Record<string, unknown>,
      })
      return { status: res.status, headers: res.headers, data: res._data }
    }
    catch (err: any) {
      return {
        status: err?.response?.status ?? err?.statusCode ?? 503,
        headers: err?.response?.headers ?? new Headers(),
        data: err?.response?._data ?? err?.data,
      }
    }
  }

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
