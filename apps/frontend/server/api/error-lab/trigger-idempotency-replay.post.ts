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

  // Find an active merchant with create capability
  let merchantId: string | null = null
  const merchantsResult = await callBackend('/api/merchants', {
    method: 'GET',
    headers: authHeaders(),
  })
  if (merchantsResult.status === 200 && merchantsResult.data) {
    const data = merchantsResult.data as any
    const merchants: any[] = Array.isArray(data) ? data : (data?.content ?? data?.merchants ?? [])
    const active = merchants.find((m: any) => m.status === 'ACTIVE') ?? merchants[0]
    merchantId = active?.id ?? active?.merchantId ?? null
  }

  if (!merchantId) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return {
      type: 'https://api.payment-quality.local/problems/lab-unavailable',
      title: 'No active merchant',
      status: 503,
      detail: 'Idempotency replay trigger requires an active merchant.',
      error: 'lab_unavailable',
    }
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
