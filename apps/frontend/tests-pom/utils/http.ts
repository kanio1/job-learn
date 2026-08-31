/**
 * Shared HTTP parse / header helpers for BffClient and REST specs.
 * Do not call seed-learning or /api/test/etl/payments/* from here.
 */
import { expect } from '@playwright/test'
import { isProblemDetails, problemDetailsSchema, type ProblemDetails } from './problem'
import { z } from 'zod'

export function parseJsonWithSchema<T>(text: string, schema: z.ZodType<T>, context: string): T {
  if (!text) {
    throw new Error(`${context} returned an empty JSON body`)
  }
  let value: unknown
  try {
    value = JSON.parse(text)
  }
  catch (error) {
    throw new Error(`${context} returned malformed JSON: ${error instanceof Error ? error.message : String(error)}`)
  }
  const parsed = schema.safeParse(value)
  if (!parsed.success) {
    throw new Error(`${context} returned unexpected JSON: ${parsed.error.message}`)
  }
  return parsed.data
}

export function headerOf(headers: Record<string, string>, name: string): string | undefined {
  return headers[name.toLowerCase()]
}

export function etagOf(headers: Record<string, string>): string | undefined {
  return headerOf(headers, 'etag')
}

/** Assert a response carries an ETag and return it (specs keep the oracle). */
export function requireEtag(headers: Record<string, string>): string {
  const etag = etagOf(headers)
  if (!etag) {
    throw new Error('response must carry an ETag')
  }
  return etag
}

export function locationOf(headers: Record<string, string>): string | undefined {
  return headerOf(headers, 'location')
}

export function correlationIdOf(headers: Record<string, string>): string | undefined {
  return headerOf(headers, 'x-correlation-id')
}

// oxlint-disable-next-line anti-slop/no-unknown-parameters -- Zod validates the external response before use.
export function expectProblem(
  // oxlint-disable-next-line anti-slop/no-unknown-parameters -- Zod validates the external response before use.
  body: unknown,
  status: number,
  error?: string,
): asserts body is ProblemDetails {
  const parsed = problemDetailsSchema.safeParse(body)
  expect(isProblemDetails(body), 'response body must be problem+json').toBe(true)
  if (!parsed.success) {
    throw new Error('response body must be problem+json')
  }
  const problem = parsed.data
  expect(problem.status, `problem.status must be ${status}`).toBe(status)
  expect(
    (problem.title && problem.title.length > 0) || (problem.detail && problem.detail.length > 0),
    'problem title or detail must be non-blank',
  ).toBe(true)
  if (error) {
    expect(problem.error, `problem.error must be ${error}`).toBe(error)
  }
}

/** Merchant registry 4xx is `ErrorResponse.error`, not problem+json. */
// oxlint-disable-next-line anti-slop/no-unknown-parameters -- Zod validates the external response before use.
export function expectMerchantError(body: unknown, error: string): void {
  const parsed = z.object({ error: z.string() }).passthrough().safeParse(body)
  expect(parsed.success, 'merchant response body must include error').toBe(true)
  if (!parsed.success) {
    throw new Error('merchant response body must include error')
  }
  expect(parsed.data.error, `merchant ErrorResponse.error must be ${error}`).toBe(error)
}

export function expectNoAuthTokenLeak(headers: Record<string, string>, rawBody: string): void {
  expect(headers['authorization'], 'Authorization must not appear on the response').toBeUndefined()
  expect(rawBody.includes('Bearer eyJ'), 'body must not leak a Bearer JWT').toBe(false)
}
