/**
 * Error Lab trigger proxy route.
 *
 * Accepts { scenario: '400'|'401'|'403'|'404'|'406'|'409'|'412'|'415'|'428' }
 * and performs the backend call designed to trigger that specific HTTP error status.
 *
 * Security: the bearer token is NEVER returned in the response body or headers.
 * The Authorization header is attached server-side and never forwarded to the browser.
 *
 * Requirements: 6.1, 6.2, 6.3, 6.5, 6.6, 6.7
 */

import type { H3Event } from 'h3'

const SUPPORTED_SCENARIOS = ['400', '401', '403', '404', '406', '409', '412', '415', '428'] as const
type Scenario = typeof SUPPORTED_SCENARIOS[number]

/** Forward relevant error-context headers from the backend response to the browser. */
function forwardHeaders(event: H3Event, headers: Headers) {
  for (const name of ['Content-Type', 'ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Allow', 'Accept-Patch']) {
    const value = headers.get(name) ?? headers.get(name.toLowerCase())
    if (value) setHeader(event, name, value)
  }
}

export default defineEventHandler(async (event) => {
  const body = await readBody<{ scenario?: string }>(event)
  const scenario = body?.scenario as Scenario | undefined

  if (!scenario || !SUPPORTED_SCENARIOS.includes(scenario)) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Unsupported or missing scenario',
      data: { error: 'unsupported_scenario', supported: SUPPORTED_SCENARIOS },
    })
  }

  const config = useRuntimeConfig()
  const backendUrl = (config.public.apiBaseUrl as string) || 'http://localhost:8080'

  // Obtain the session token for scenarios that need auth
  let accessToken: string | undefined
  try {
    const session = await requireUserSession(event)
    accessToken = session.secure?.accessToken
  } catch {
    // 401 scenario intentionally has no valid session — that's fine
  }

  /** Makes a raw backend call and returns { status, headers, body } */
  async function callBackend(
    path: string,
    opts: {
      method?: string
      headers?: Record<string, string>
      body?: unknown
    } = {}
  ): Promise<{ status: number; headers: Headers; data: unknown }> {
    try {
      const res = await $fetch.raw(`${backendUrl}${path}`, {
        method: (opts.method ?? 'GET') as any,
        headers: opts.headers,
        body: opts.body as any,
      })
      return { status: res.status, headers: res.headers, data: res._data }
    } catch (err: any) {
      // $fetch.raw throws on non-2xx; we want to capture and return the error response
      const status: number = err?.response?.status ?? err?.statusCode ?? 503
      const headers: Headers = err?.response?.headers ?? new Headers()
      const data: unknown = err?.response?._data ?? err?.data ?? {
        type: 'about:blank',
        title: 'Service Unavailable',
        status,
        detail: 'The backend could not be reached.',
      }
      return { status, headers, data }
    }
  }

  /** Authenticated headers helper */
  function authHeaders(extra: Record<string, string> = {}): Record<string, string> {
    return {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...extra,
    }
  }

  let result: { status: number; headers: Headers; data: unknown }

  switch (scenario) {
    // 400 — invalid request body (negative amountMinor violates validation)
    case '400': {
      result = await callBackend(
        `/api/merchants/error-lab-merchant/payment-orders`,
        {
          method: 'POST',
          headers: authHeaders({ 'Idempotency-Key': `error-lab-400-${Date.now()}` }),
          body: {
            amountMinor: -1,
            currency: 'PLN',
            clientOrderReference: 'error-lab-invalid',
          },
        }
      )
      break
    }

    // 401 — call a protected endpoint WITHOUT any Authorization header
    case '401': {
      result = await callBackend(
        `/api/merchants`,
        {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
          // Deliberately no Authorization header
        }
      )
      break
    }

    // 403 — call an endpoint that requires platform-level authority
    // A merchant-scoped or missing authority results in 403
    case '403': {
      // POST /api/merchants requires platform:merchants:create authority.
      // We call it with the real token but with a body that would be valid —
      // if the current user lacks the authority (or we send an invalid token hint),
      // the backend returns 403. In practice the platform-operator has this right,
      // so we use a deliberately crafted token placeholder to force 403.
      // Best reliable approach: call the merchants endpoint with a fake token so backend returns 403.
      result = await callBackend(
        `/api/merchants`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer error-lab-invalid-token-for-403',
          },
          body: {
            merchantReference: 'error-lab-forbidden',
            displayName: 'Error Lab Forbidden',
          },
        }
      )
      break
    }

    // 404 — request a non-existent resource ID
    case '404': {
      result = await callBackend(
        `/api/merchants/nonexistent-error-lab-id-00000000`,
        {
          method: 'GET',
          headers: authHeaders(),
        }
      )
      break
    }

    // 406 — send Accept: application/xml (not supported)
    case '406': {
      result = await callBackend(
        `/api/merchants`,
        {
          method: 'GET',
          headers: {
            ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
            Accept: 'application/xml',
          },
        }
      )
      break
    }

    // 409 — same Idempotency-Key with different payload
    case '409': {
      const idempotencyKey = `error-lab-409-${Date.now()}`
      const merchantId = 'error-lab-merchant-409'

      // First call with payload A
      await callBackend(
        `/api/merchants/${merchantId}/payment-orders`,
        {
          method: 'POST',
          headers: authHeaders({ 'Idempotency-Key': idempotencyKey }),
          body: {
            amountMinor: 1000,
            currency: 'PLN',
            clientOrderReference: 'error-lab-conflict-a',
          },
        }
      )

      // Second call with same key but different payload — should return 409
      result = await callBackend(
        `/api/merchants/${merchantId}/payment-orders`,
        {
          method: 'POST',
          headers: authHeaders({ 'Idempotency-Key': idempotencyKey }),
          body: {
            amountMinor: 2000,
            currency: 'EUR',
            clientOrderReference: 'error-lab-conflict-b',
          },
        }
      )
      break
    }

    // 412 — send a stale/fake If-Match on a lifecycle action
    case '412': {
      result = await callBackend(
        `/api/merchants/error-lab-merchant/payment-orders/error-lab-order-412/authorize`,
        {
          method: 'POST',
          headers: authHeaders({
            'If-Match': '"stale-etag-0"',
            'Idempotency-Key': `error-lab-412-${Date.now()}`,
          }),
          body: {},
        }
      )
      break
    }

    // 415 — wrong Content-Type (text/plain)
    case '415': {
      result = await callBackend(
        `/api/merchants`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain',
            ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
          },
          body: 'this is plain text, not json' as any,
        }
      )
      break
    }

    // 428 — omit If-Match on a lifecycle action that requires it
    case '428': {
      result = await callBackend(
        `/api/merchants/error-lab-merchant/payment-orders/error-lab-order-428/authorize`,
        {
          method: 'POST',
          headers: authHeaders({
            'Idempotency-Key': `error-lab-428-${Date.now()}`,
            // Deliberately NO If-Match header
          }),
          body: {},
        }
      )
      break
    }

    default: {
      throw createError({ statusCode: 400, statusMessage: 'Unsupported scenario' })
    }
  }

  // Forward relevant headers (excluding Authorization — never expose the token)
  forwardHeaders(event, result.headers)

  // Set the response status to the backend's actual status
  setResponseStatus(event, result.status)

  return result.data
})
