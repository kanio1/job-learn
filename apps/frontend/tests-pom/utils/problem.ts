export type ProblemDetails = {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  error?: string
}

export function isProblemDetails(body: unknown): body is ProblemDetails {
  if (!body || typeof body !== 'object') {
    return false
  }
  const record = body as Record<string, unknown>
  const status = record.status
  if (typeof status === 'number' && status >= 400) {
    return true
  }
  return typeof record.type === 'string' && record.type.startsWith('http')
}
