export default defineEventHandler((event) => {
  requireMirrorLab(event)
  const key = sessionKey(event)
  const n = nextRetryAttempt(key)
  if (n === 1) {
    setHeader(event, 'Retry-After', 1)
    setResponseStatus(event, 503)
    return { error: 'service_unavailable', message: 'Retry once' }
  }
  clearRetryAttempt(key)
  return { status: 'ok', attempts: n }
})
