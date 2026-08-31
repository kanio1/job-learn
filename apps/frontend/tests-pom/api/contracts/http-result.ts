import { z } from 'zod'

export type HttpHeaders = Readonly<Record<string, string>>

export type JsonSuccess<T> = {
  kind: 'success'
  status: number
  body: T
  headers: HttpHeaders
}

export type JsonError<T> = {
  kind: 'error'
  status: number
  body: T
  headers: HttpHeaders
}

export type JsonResult<TSuccess, TError> = JsonSuccess<TSuccess> | JsonError<TError>

export type EmptyResult = {
  kind: 'empty'
  status: number
  headers: HttpHeaders
}

/** Non-JSON downloads remain explicit rather than pretending to be JSON or empty. */
export type TextResult = {
  kind: 'text'
  status: number
  body: string
  headers: HttpHeaders
}

export type BinaryResult = {
  kind: 'binary'
  status: number
  body: Buffer
  headers: HttpHeaders
}

type BaseResponseSchemas<TSuccess, TError> = {
  success: { statuses: readonly number[], schema: z.ZodType<TSuccess> }
  error: { statuses: readonly number[], schema: z.ZodType<TError> }
}

export type JsonResponseSchemas<TSuccess, TError> = BaseResponseSchemas<TSuccess, TError> & {
  empty?: never
}

export type ResponseSchemas<TSuccess, TError> = BaseResponseSchemas<TSuccess, TError> & {
  empty: { statuses: readonly number[] }
}

type ResponseInput = {
  endpoint: string
  status: number
  text: string
  headers: HttpHeaders
}

export function decodeResponse<TSuccess, TError>(
  input: ResponseInput,
  schemas: JsonResponseSchemas<TSuccess, TError>,
): JsonResult<TSuccess, TError>
export function decodeResponse<TSuccess, TError>(
  input: ResponseInput,
  schemas: ResponseSchemas<TSuccess, TError>,
): JsonResult<TSuccess, TError> | EmptyResult
export function decodeResponse<TSuccess, TError>(
  input: ResponseInput,
  schemas: JsonResponseSchemas<TSuccess, TError> | ResponseSchemas<TSuccess, TError>,
): JsonResult<TSuccess, TError> | EmptyResult {
  if ('empty' in schemas && schemas.empty !== undefined && schemas.empty.statuses.includes(input.status)) {
    if (input.text.length > 0) {
      throw new Error(`${input.endpoint} ${input.status} must not return a response body`)
    }
    return { kind: 'empty', status: input.status, headers: input.headers }
  }

  if (schemas.success.statuses.includes(input.status)) {
    return {
      kind: 'success',
      status: input.status,
      body: parseBody(input, schemas.success.schema, 'success'),
      headers: input.headers,
    }
  }

  if (schemas.error.statuses.includes(input.status)) {
    return {
      kind: 'error',
      status: input.status,
      body: parseBody(input, schemas.error.schema, 'error'),
      headers: input.headers,
    }
  }

  throw new Error(`${input.endpoint} returned unexpected status ${input.status}`)
}

function parseBody<T>(input: ResponseInput, schema: z.ZodType<T>, expectedKind: 'success' | 'error'): T {
  if (input.text.length === 0) {
    throw new Error(`${input.endpoint} ${input.status} returned an empty ${expectedKind} JSON body`)
  }

  let value: unknown
  try {
    value = JSON.parse(input.text)
  }
  catch (error) {
    const detail = error instanceof Error ? error.message : String(error)
    throw new Error(`${input.endpoint} ${input.status} returned malformed ${expectedKind} JSON: ${detail}`)
  }

  const parsed = schema.safeParse(value)
  if (!parsed.success) {
    throw new Error(`${input.endpoint} ${input.status} returned invalid ${expectedKind} JSON: ${parsed.error.message}`)
  }
  return parsed.data
}

export function expectSuccess<TSuccess, TError>(
  result: JsonResult<TSuccess, TError> | EmptyResult,
  expectedStatus: number,
): JsonSuccess<TSuccess> {
  if (result.status !== expectedStatus) {
    throw new Error(`Expected status ${expectedStatus}, got ${result.status}`)
  }
  if (result.kind !== 'success') {
    throw new Error(`Expected ${expectedStatus} success response, got ${result.kind} response`)
  }
  return result
}

export function expectError<TSuccess, TError>(
  result: JsonResult<TSuccess, TError> | EmptyResult,
  expectedStatus: number,
): JsonError<TError> {
  if (result.status !== expectedStatus) {
    throw new Error(`Expected status ${expectedStatus}, got ${result.status}`)
  }
  if (result.kind !== 'error') {
    throw new Error(`Expected ${expectedStatus} error response, got ${result.kind} response`)
  }
  return result
}

export function expectEmpty<TSuccess, TError>(
  result: JsonResult<TSuccess, TError> | EmptyResult,
  expectedStatus: number,
): EmptyResult {
  if (result.status !== expectedStatus) {
    throw new Error(`Expected status ${expectedStatus}, got ${result.status}`)
  }
  if (result.kind !== 'empty') {
    throw new Error(`Expected ${expectedStatus} empty response, got ${result.kind} response`)
  }
  return result
}
