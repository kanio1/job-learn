/**
 * Error Lab — 412 Precondition Failed.
 * Create a real order, then authorize with stale If-Match `"v99"`.
 */
import { labUnavailableBody, merchantIdForLabTrigger } from '../../utils/errorLabBackend'

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
  const merchantId = merchantIdForLabTrigger(merchantsResult.data)

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
        clientOrderReference: `error-lab-412-${Date.now()}`,
      },
    },
  )

  const createdOrder = createResult.data as { id?: string, paymentOrderId?: string } | undefined
  const paymentOrderId = createdOrder?.id ?? createdOrder?.paymentOrderId
  if ((createResult.status !== 201 && createResult.status !== 200) || !paymentOrderId) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return labUnavailableBody('Could not create a payment order for the 412 trigger.')
  }

  const result = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/authorize`,
    {
      method: 'POST',
      headers: authHeaders({
        'If-Match': '"v99"',
        'Idempotency-Key': `error-lab-412-auth-${Date.now()}`,
      }),
      body: {},
    },
  )

  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Content-Type']) {
    const val = result.headers.get(name)
    if (val) {
      setHeader(event, name, val)
    }
  }
  setResponseStatus(event, result.status)
  return result.data
})
