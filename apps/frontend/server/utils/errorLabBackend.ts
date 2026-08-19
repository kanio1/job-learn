import type { H3Event } from 'h3'

const PROBLEM_TYPE = 'https://api.payment-quality.local/problems/lab-unavailable'

/** Seeded Alpha merchant used when the session cannot list /api/merchants. */
export const LAB_ALPHA_MERCHANT_ID = '00000000-0000-0000-0000-0000000000b1'

export function merchantIdFromListPayload(data: unknown): string | null {
  if (!data) {
    return null
  }
  const record = data as { content?: unknown[], merchants?: unknown[] }
  const merchants: unknown[] = Array.isArray(data)
    ? data
    : (Array.isArray(record.content) ? record.content : record.merchants ?? [])
  if (merchants.length === 0) {
    return null
  }
  const typed = merchants as { status?: string, id?: string, merchantId?: string }[]
  const active = typed.find(merchant => merchant.status === 'ACTIVE') ?? typed[0]
  return active?.id ?? active?.merchantId ?? null
}

export function sessionMerchantId(session: { user?: { merchantId?: string } } | null | undefined): string | undefined {
  const id = session?.user?.merchantId
  return typeof id === 'string' && id.length > 0 ? id : undefined
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
