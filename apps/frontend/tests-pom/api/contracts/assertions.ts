import { expect } from '@playwright/test'
import type { EmptyResult, JsonResult, JsonSuccess } from './http-result'

type SuccessStatus = 200 | 201 | 202
type StatusResult = JsonResult<unknown, unknown> | EmptyResult

/** Assert a status and narrow only successful JSON responses. */
export function expectStatus<TSuccess, TError>(
  result: JsonResult<TSuccess, TError> | EmptyResult,
  expected: SuccessStatus,
  message?: string,
): asserts result is JsonSuccess<TSuccess>
export function expectStatus(
  result: StatusResult,
  expected: number,
  message?: string,
): void
export function expectStatus(
  result: StatusResult,
  expected: number,
  message?: string,
): void {
  expect(result.status, message).toBe(expected)
  if (result.status === expected && expected >= 200 && expected < 300 && result.kind === 'error') {
    throw new Error(`Expected ${expected} success response, got error response`)
  }
}
