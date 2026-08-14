/**
 * Error Lab — 304 Not Modified trigger.
 *
 * Two-step flow:
 * 1. GET payment order → capture ETag
 * 2. GET same payment order with If-None-Match: ETag → backend returns 304
 *
 * Requires: BE-MVP-001 (If-None-Match support) and BFF-MVP-002 (forward If-None-Match).
 * Requirements: 6.1, Error Lab MVP
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
    opts: { method?: string, headers?: Record<string, string>, body?: unknown } = {},
  ): Promise<{ status: number, headers: Headers, data: unknown }> {
    try {
      const res = await $fetch.raw(`${backendUrl}${path}`, {
        method: (opts.method ?? 'GET') as 'GET' | 'POST',
        headers: opts.headers,
        body: opts.body as Record<string, unknown> | undefined,
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

  const merchantsResult = await callBackend('/api/merchants', { headers: authHeaders() })
  const merchantId = merchantIdForLabTrigger(merchantsResult.data)

  let paymentOrderId: string | null = null
  const ordersResult = await callBackend(`/api/merchants/${merchantId}/payment-orders`, {
    headers: authHeaders(),
  })
  if (ordersResult.status === 200 && ordersResult.data) {
    const data = ordersResult.data as { content?: unknown[], orders?: unknown[] }
    const orders: unknown[] = Array.isArray(ordersResult.data)
      ? ordersResult.data
      : (data.content ?? data.orders ?? [])
    const first = orders[0] as { id?: string, paymentOrderId?: string } | undefined
    paymentOrderId = first?.id ?? first?.paymentOrderId ?? null
  }

  if (!paymentOrderId) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return labUnavailableBody('No payment order is available for the 304 trigger; seed Alpha first.')
  }

  // Step 2: GET to capture ETag
  const firstGet = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
    { headers: authHeaders() }
  )
  const etag = firstGet.headers.get('etag') ?? firstGet.headers.get('ETag')

  if (!etag) {
    setResponseStatus(event, 503)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return {
      type: 'https://api.payment-quality.local/problems/lab-unavailable',
      title: 'No ETag received',
      status: 503,
      detail: 'First GET did not return an ETag. Conditional GET cannot proceed.',
      error: 'lab_unavailable',
    }
  }

  // Step 3: GET with If-None-Match → backend returns 304
  const conditionalGet = await callBackend(
    `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
    { headers: authHeaders({ 'If-None-Match': etag }) }
  )

  for (const name of ['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Last-Modified']) {
    const val = conditionalGet.headers.get(name)
    if (val) setHeader(event, name, val)
  }

  setResponseStatus(event, conditionalGet.status)
  return conditionalGet.data ?? null
})
