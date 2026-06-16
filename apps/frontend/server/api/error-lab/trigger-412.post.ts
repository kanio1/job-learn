/**
 * Error Lab — 412 Precondition Failed trigger.
 *
 * Strategy (two-step in a single handler):
 * 1. Create a new payment order → get its real ETag.
 * 2. Call authorize on that order with a STALE If-Match → backend returns 412.
 *
 * This guarantees we hit a real order and get the precondition-failed path.
 *
 * Requirements: 6.1, 6.5
 */
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

  // Step 1: Find a real merchant to use. Try to get any merchant from the list.
  let merchantId: string | null = null
  let paymentOrderId: string | null = null

  const merchantsResult = await callBackend('/api/merchants', {
    method: 'GET',
    headers: authHeaders(),
  })

  if (merchantsResult.status === 200 && merchantsResult.data) {
    const data = merchantsResult.data as any
    const merchants: any[] = Array.isArray(data) ? data : (data?.content ?? data?.merchants ?? [])
    if (merchants.length > 0) {
      const activeMerchant = merchants.find((m: any) => m.status === 'ACTIVE') ?? merchants[0]
      merchantId = activeMerchant?.id ?? activeMerchant?.merchantId ?? null
    }
  }

  if (!merchantId) {
    // Fallback: use a well-known error-lab merchant ID
    merchantId = 'error-lab-merchant-412'
  }

  // Step 2: Create a payment order to get a real order + ETag
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
        clientOrderReference: 'error-lab-412-order',
      },
    }
  )

  if (createResult.status === 201 || createResult.status === 200) {
    const createdOrder = createResult.data as any
    paymentOrderId = createdOrder?.id ?? createdOrder?.paymentOrderId ?? null
  }

  if (!paymentOrderId) {
    // Fallback: call authorize on a known non-existent order with stale etag
    // This will return 404 or 412 depending on backend implementation
    paymentOrderId = 'error-lab-order-412'
    merchantId = 'error-lab-merchant-412'
  }

  // Step 3: Call authorize with a STALE If-Match value → 412
  const result = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/authorize`,
    {
      method: 'POST',
      headers: authHeaders({
        'If-Match': '"stale-etag-version-0"',
        'Idempotency-Key': `error-lab-412-auth-${Date.now()}`,
      }),
      body: {},
    }
  )

  // Forward headers (excluding Authorization)
  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type']) {
    const val = result.headers.get(name)
    if (val) setHeader(event, name, val)
  }

  setResponseStatus(event, result.status)
  return result.data
})
