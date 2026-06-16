/**
 * Error Lab — 428 Precondition Required trigger.
 *
 * Strategy (two-step in a single handler):
 * 1. Create a new payment order (to get a real order that accepts authorize).
 * 2. Call authorize WITHOUT the If-Match header → backend returns 428.
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

  // Step 1: Find a real merchant
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
    merchantId = 'error-lab-merchant-428'
  }

  // Step 2: Create a payment order to get a real orderId
  const createResult = await callBackend(
    `/api/merchants/${merchantId}/payment-orders`,
    {
      method: 'POST',
      headers: authHeaders({
        'Idempotency-Key': `error-lab-428-create-${Date.now()}`,
      }),
      body: {
        amountMinor: 500,
        currency: 'PLN',
        clientOrderReference: 'error-lab-428-order',
      },
    }
  )

  if (createResult.status === 201 || createResult.status === 200) {
    const createdOrder = createResult.data as any
    paymentOrderId = createdOrder?.id ?? createdOrder?.paymentOrderId ?? null
  }

  if (!paymentOrderId) {
    paymentOrderId = 'error-lab-order-428'
    merchantId = 'error-lab-merchant-428'
  }

  // Step 3: Call authorize WITHOUT If-Match → 428
  const result = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/authorize`,
    {
      method: 'POST',
      headers: authHeaders({
        'Idempotency-Key': `error-lab-428-auth-${Date.now()}`,
        // Deliberately NO If-Match header
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
