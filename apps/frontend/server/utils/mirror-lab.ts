import type { H3Event } from 'h3'
import { createError, getCookie, getHeader, getRequestIP } from 'h3'

type DeviceRecord = { id: string, label: string, createdAt: string }
type SessionDevices = Map<string, DeviceRecord[]>

const devicesBySession: SessionDevices = ((globalThis as any).__mrlDevices ??= new Map())

export function requireMirrorLab(event: H3Event) {
  const config = useRuntimeConfig(event)
  if (config.public.mirrorLabEnabled !== true) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }
}

export function sessionKey(event: H3Event): string {
  return getCookie(event, 'nuxt-session') || getRequestIP(event, { xForwardedFor: true }) || 'anon'
}

export async function requireMirrorLabSession(event: H3Event) {
  requireMirrorLab(event)
  await requireUserSession(event)
}

type RetryEntry = { count: number, at: number }
const retryEntries: Map<string, RetryEntry> = ((globalThis as any).__mrlRetry ??= new Map())
const RETRY_TTL_MS = 10_000

export function nextRetryAttempt(key: string): number {
  const now = Date.now()
  const previous = retryEntries.get(key)
  if (!previous || now - previous.at > RETRY_TTL_MS) {
    retryEntries.set(key, { count: 1, at: now })
    return 1
  }
  const count = previous.count + 1
  retryEntries.set(key, { count, at: now })
  return count
}

export function clearRetryAttempt(key: string) {
  retryEntries.delete(key)
}

export function listDevices(session: string): DeviceRecord[] {
  return devicesBySession.get(session) ?? []
}

export function registerDevice(session: string, id: string, label: string): DeviceRecord {
  const existing = devicesBySession.get(session) ?? []
  const found = existing.find(device => device.id === id)
  if (found) {
    return found
  }
  const record = { id, label, createdAt: new Date().toISOString() }
  devicesBySession.set(session, [...existing, record])
  return record
}

export function revokeDevice(session: string, id: string): boolean {
  const existing = devicesBySession.get(session) ?? []
  const next = existing.filter(device => device.id !== id)
  devicesBySession.set(session, next)
  return next.length !== existing.length
}

export function csrfTokenFrom(event: H3Event): string | undefined {
  return getCookie(event, 'mrl-csrf') || undefined
}

export function problemJson(
  event: H3Event,
  status: number,
  error: string,
  detail: string,
) {
  const correlationId = getHeader(event, 'x-correlation-id') || crypto.randomUUID()
  setResponseStatus(event, status)
  setHeader(event, 'Content-Type', 'application/problem+json')
  setHeader(event, 'X-Correlation-ID', correlationId)
  return {
    type: `https://api.payment-quality.local/problems/${error.replaceAll('_', '-')}`,
    title: status === 403 ? 'Forbidden' : 'Error',
    status,
    detail,
    error,
    correlationId,
  }
}
