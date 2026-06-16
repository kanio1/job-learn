/**
 * Error Lab — 409 Conflict (Idempotency) trigger.
 *
 * Sends the same Idempotency-Key twice with different payloads.
 * The backend detects the key/payload mismatch and returns 409.
 *
 * Strategy: first call uses a fresh key with payload A; second call (which is
 * what the browser receives) uses the same key with payload B → 409 conflict.
 *
 * Requirements: 6.1, 6.5
 */

// Module-level store so the same key is reused across calls to this route handler.
// The key is refreshed each time the first call succeeds (or errors non-409).
let storedIdempotencyKey: string | null = null

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
    opts: { method?: string; headers?: Record<string, string>; body?: unknown }
  ): Promise<{ status: number; headers: Headers; data: unknown }> {
    try {
      const res = await $fetch.raw(`${backendUrl}${path}`, {
        method: (opts.method ?? 'POST') as any,
        headers: opts.headers,
        body: opts.body as any,
      })
      return { status: res.status, headers: res.headers, data: res._data }
    } catch (err: any) {
      return {
        status: err?.response?.status ?? err?.statusCode ?? 503,
        headers: err?.response?.headers ?? new Headers(),
        data: err?.response?._data ?? err?.data,
      }
    }
  }

  const merchantId = 'error-lab-merchant-409'

  // If we don't have a stored key, generate one and make the FIRST call (payload A)
  if (!storedIdempotencyKey) {
    const newKey = `error-lab-409-${Date.now()}`
    await callBackend(`/api/merchants/${merchantId}/payment-orders`, {
      method: 'POST',
      headers: authHeaders({ 'Idempotency-Key': newKey }),
      body: {
        amountMinor: 1000,
        currency: 'PLN',
        clientOrderReference: 'error-lab-conflict-a',
      },
    })
    storedIdempotencyKey = newKey
  }

  // Second call: same Idempotency-Key but different payload → 409
  const result = await callBackend(`/api/merchants/${merchantId}/payment-orders`, {
    method: 'POST',
    headers: authHeaders({ 'Idempotency-Key': storedIdempotencyKey }),
    body: {
      amountMinor: 2000,
      currency: 'EUR',
      clientOrderReference: 'error-lab-conflict-b',
    },
  })

  // If we got 409, clear the stored key so next trigger generates a new pair
  if (result.status === 409) {
    storedIdempotencyKey = null
  }

  // Forward headers (excluding Authorization)
  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type']) {
    const val = result.headers.get(name)
    if (val) setHeader(event, name, val)
  }

  setResponseStatus(event, result.status)
  return result.data
})
