/**
 * Shared HTTP parse / header helpers for BffClient and REST specs.
 * Do not call seed-learning or /api/test/etl/payments/* from here.
 */
import { expect } from '@playwright/test'
import { isProblemDetails, type ProblemDetails } from './problem'

export function parseJsonText<T>(text: string): T | undefined {
  if (!text) {
    return undefined
  }
  try {
    // SAFETY: caller names T; invalid JSON is rejected in catch.
    return JSON.parse(text) as T
  }
  catch {
    return undefined
  }
}

export async function parseJson<T>(response: { text(): Promise<string> }): Promise<T | undefined> {
  return parseJsonText<T>(await response.text())
}

export function headerOf(headers: Record<string, string>, name: string): string | undefined {
  return headers[name.toLowerCase()]
}

export function etagOf(headers: Record<string, string>): string | undefined {
  return headerOf(headers, 'etag')
}

export function locationOf(headers: Record<string, string>): string | undefined {
  return headerOf(headers, 'location')
}

export function correlationIdOf(headers: Record<string, string>): string | undefined {
  return headerOf(headers, 'x-correlation-id')
}

export function expectProblem(
  body: unknown,
  status: number,
  error?: string,
): asserts body is ProblemDetails {
  expect(isProblemDetails(body), 'response body must be problem+json').toBe(true)
  const problem = body as ProblemDetails
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
export function expectMerchantError(body: unknown, error: string): void {
  const record = body && typeof body === 'object' ? body as Record<string, unknown> : {}
  expect(record.error, `merchant ErrorResponse.error must be ${error}`).toBe(error)
}

export function expectNoAuthTokenLeak(headers: Record<string, string>, rawBody: string): void {
  expect(headers['authorization'], 'Authorization must not appear on the response').toBeUndefined()
  expect(rawBody.includes('Bearer eyJ'), 'body must not leak a Bearer JWT').toBe(false)
}
