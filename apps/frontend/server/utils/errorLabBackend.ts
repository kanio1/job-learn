import type { H3Event } from 'h3'

const PROBLEM_TYPE = 'https://api.payment-quality.local/problems/lab-unavailable'
const DEFAULT_BACKEND_URL = 'http://localhost:8080'

export type LabHttpMethod = 'GET' | 'POST' | 'PATCH' | 'PUT' | 'HEAD'

export type LabBackendResult = {
  status: number
  headers: Headers
  data: unknown
}

export type LabSession = {
  user?: { merchantId?: string }
  secure?: { accessToken?: string }
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0
}

function isPlainObject(value: unknown): value is { content?: unknown, merchants?: unknown, orders?: unknown } {
  return typeof value === 'object' && value !== null
}

function isMerchantRow(value: unknown): value is { status?: string, id?: string, merchantId?: string, paymentOrderId?: string } {
  return typeof value === 'object' && value !== null
}

function isFetchFailure(value: unknown): value is {
  response?: { status?: number, headers?: Headers, _data?: unknown }
  statusCode?: number
  data?: unknown
} {
  return typeof value === 'object' && value !== null
}

export function labBackendUrl(): string {
  const base = useRuntimeConfig().public.apiBaseUrl
  return isNonEmptyString(base) ? base : DEFAULT_BACKEND_URL
}

export function sessionAccessToken(session: LabSession | null | undefined): string | undefined {
  const token = session?.secure?.accessToken
  return isNonEmptyString(token) ? token : undefined
}

export function labAuthHeaders(
  accessToken: string | undefined,
  extra: Record<string, string> = {},
): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  for (const [key, value] of Object.entries(extra)) {
    headers[key] = value
  }
  return headers
}

export async function callLabBackend(
  backendUrl: string,
  path: string,
  opts: {
    method?: LabHttpMethod
    headers?: Record<string, string>
    body?: Record<string, string | number | boolean | null>
  } = {},
): Promise<LabBackendResult> {
  try {
    const res = await $fetch.raw(`${backendUrl}${path}`, {
      method: opts.method ?? 'GET',
      headers: opts.headers,
      body: opts.body,
    })
    return { status: res.status, headers: res.headers, data: res._data }
  }
  catch (err: unknown) {
    const failure = isFetchFailure(err) ? err : {}
    return {
      status: failure.response?.status ?? failure.statusCode ?? 503,
      headers: failure.response?.headers ?? new Headers(),
      data: failure.response?._data ?? failure.data,
    }
  }
}

export function createLabBackendClient(session: LabSession | null | undefined) {
  const backendUrl = labBackendUrl()
  const accessToken = sessionAccessToken(session)
  return {
    backendUrl,
    accessToken,
    authHeaders(extra: Record<string, string> = {}) {
      return labAuthHeaders(accessToken, extra)
    },
    callBackend(
      path: string,
      opts: {
        method?: LabHttpMethod
        headers?: Record<string, string>
        body?: Record<string, string | number | boolean | null>
      } = {},
    ) {
      return callLabBackend(backendUrl, path, opts)
    },
  }
}

export function labCreatedOrderId(data: unknown): string | undefined {
  if (!isMerchantRow(data)) {
    return undefined
  }
  return data.id ?? data.paymentOrderId ?? data.merchantId
}

export function labFirstOrderId(data: unknown): string | undefined {
  if (Array.isArray(data)) {
    return labCreatedOrderId(data[0])
  }
  if (!isPlainObject(data)) {
    return undefined
  }
  if (Array.isArray(data.content)) {
    return labCreatedOrderId(data.content[0])
  }
  if (Array.isArray(data.orders)) {
    return labCreatedOrderId(data.orders[0])
  }
  if (Array.isArray(data.merchants)) {
    return labCreatedOrderId(data.merchants[0])
  }
  return undefined
}

/** Seeded Alpha merchant used when the session cannot list /api/merchants. */
export const LAB_ALPHA_MERCHANT_ID = '00000000-0000-0000-0000-0000000000b1'

function firstMerchantId(values: unknown[]): string | null {
  const rows = values.filter(isMerchantRow)
  if (rows.length === 0) {
    return null
  }
  const active = rows.find(merchant => merchant.status === 'ACTIVE') ?? rows[0]
  return active?.id ?? active?.merchantId ?? null
}

export function merchantIdFromListPayload(data: unknown): string | null {
  if (Array.isArray(data)) {
    return firstMerchantId(data)
  }
  if (!isPlainObject(data)) {
    return null
  }
  if (Array.isArray(data.content)) {
    return firstMerchantId(data.content)
  }
  if (Array.isArray(data.merchants)) {
    return firstMerchantId(data.merchants)
  }
  return null
}

export function sessionMerchantId(session: LabSession | null | undefined): string | undefined {
  const id = session?.user?.merchantId
  return isNonEmptyString(id) ? id : undefined
}

export function merchantIdForLabTrigger(listData: unknown, ownedMerchantId?: string): string {
  return merchantIdFromListPayload(listData) ?? ownedMerchantId ?? LAB_ALPHA_MERCHANT_ID
}

export function labUnavailableBody(detail: string) {
  return {
    type: PROBLEM_TYPE,
    title: 'Error Lab cannot complete this trigger',
    status: 503,
    detail,
    error: 'lab_unavailable',
  }
}

const FORWARDED_HEADER_NAMES = [
  'ETag',
  'Cache-Control',
  'Vary',
  'X-Correlation-ID',
  'Content-Type',
  'WWW-Authenticate',
  'Location',
] as const

/** Forward Spring status, headers, and body without minting problem+json. */
export function forwardLabBackendError(event: H3Event, error: {
  response?: { status?: number, headers?: Headers, _data?: unknown }
  statusCode?: number
  data?: unknown
}) {
  const statusCode: number = error?.response?.status ?? error?.statusCode ?? 503
  const errorData = error?.response?._data ?? error?.data
  const headers = error?.response?.headers
  if (headers) {
    for (const name of FORWARDED_HEADER_NAMES) {
      const val = headers.get(name)
      if (val) {
        setHeader(event, name, val)
      }
    }
  }
  setResponseStatus(event, statusCode)
  return errorData
}
