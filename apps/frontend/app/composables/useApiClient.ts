/**
 * Header-aware API client composable.
 *
 * Wraps `$fetch.raw` so that response headers (ETag, Location, Vary,
 * Cache-Control, X-Correlation-ID, Allow, Accept-Patch) and the HTTP status
 * code are captured alongside the validated response body.
 *
 * Security: this composable only calls `server/api/**` proxy paths.
 * The bearer token is NEVER read or held client-side — it is attached
 * server-side by `backendApi.ts`.
 *
 * Requirements: 3.5, 4.1, 4.2, 7.1
 */

import type { ZodType } from 'zod'
import type { ApiResponse, ApiHeaders, ProblemDetails } from '~/types/api'
import { problemDetailsSchema } from '~/schemas/problem-details.schema'

function extractHeaders(headers: Headers | undefined): ApiHeaders {
  if (!headers) return {}
  return {
    etag: headers.get('etag') ?? undefined,
    location: headers.get('location') ?? undefined,
    vary: headers.get('vary') ?? undefined,
    cacheControl: headers.get('cache-control') ?? undefined,
    correlationId: headers.get('x-correlation-id') ?? undefined,
    allow: headers.get('allow') ?? undefined,
    acceptPatch: headers.get('accept-patch') ?? undefined,
    retryAfter: headers.get('retry-after') ?? undefined,
    wwwAuthenticate: headers.get('www-authenticate') ?? undefined,
    idempotencyReplayed: headers.get('idempotency-replayed') ?? undefined,
    lastModified: headers.get('last-modified') ?? undefined,
  }
}

function toRawString(data: unknown): string {
  if (data === undefined || data === null) return ''
  if (typeof data === 'string') return data
  return JSON.stringify(data)
}

function isProblemContentType(headers: Headers | undefined): boolean {
  if (!headers) return false
  const ct = headers.get('content-type') ?? ''
  return ct.includes('application/problem+json')
}

export function useApiClient() {
  async function request<T>(
    path: string,
    schema: ZodType<T>,
    opts?: {
      method?: string
      body?: unknown
      headers?: Record<string, string>
      query?: Record<string, unknown>
    }
  ): Promise<ApiResponse<T>> {
    try {
      const response = await $fetch.raw(path, {
        method: (opts?.method ?? 'GET') as any,
        body: opts?.body as any,
        headers: opts?.headers,
        query: opts?.query,
      })

      const status = response.status
      const apiHeaders = extractHeaders(response.headers)
      const raw = toRawString(response._data)

      // Problem response
      if (isProblemContentType(response.headers)) {
        const parsed = problemDetailsSchema.safeParse(response._data)
        const problem: ProblemDetails | null = parsed.success ? parsed.data : null
        return { data: null, status, headers: apiHeaders, problem, raw }
      }

      // Validate with supplied Zod schema
      const result = schema.safeParse(response._data)
      if (!result.success) {
        const detail = result.error.issues.map(i => i.message).join('; ')
        return {
          data: null,
          status,
          headers: apiHeaders,
          problem: {
            type: 'about:blank',
            title: 'Response Validation Error',
            status,
            detail: `Server response did not match expected schema: ${detail}`,
          },
          raw,
        }
      }

      return { data: result.data, status, headers: apiHeaders, problem: null, raw }
    } catch (err: any) {
      // $fetch.raw throws on network errors and non-2xx status codes
      const status: number = err?.statusCode ?? err?.response?.status ?? 0
      const errorData = err?.data ?? err?.response?._data
      const apiHeaders = extractHeaders(err?.response?.headers as Headers | undefined)
      const raw = toRawString(errorData)

      // Try to parse problem details from error body
      let problem: ProblemDetails | null = null
      const errorContentType: string = err?.response?.headers?.get?.('content-type') ?? ''
      if (errorContentType.includes('application/problem+json') || errorData) {
        const parsed = problemDetailsSchema.safeParse(errorData)
        if (parsed.success) {
          problem = parsed.data
        }
      }

      if (!problem && errorData) {
        // Fallback: build a minimal problem from the error
        problem = {
          type: 'about:blank',
          title: err?.statusMessage ?? 'Request Failed',
          status,
          detail: err?.message ?? 'An unexpected error occurred',
        }
      }

      return { data: null, status, headers: apiHeaders, problem, raw }
    }
  }

  return { request }
}
