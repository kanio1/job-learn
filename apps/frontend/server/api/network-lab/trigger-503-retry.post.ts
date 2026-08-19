export default defineEventHandler((event) => {
  requireMirrorLab(event)
  const key = sessionKey(event)
  const n = nextRetryAttempt(key)
  if (n === 1) {
    const retryAfter = Math.max(1, Math.ceil(retryWindowRemainingMs(key) / 1000))
    setHeader(event, 'Retry-After', retryAfter)
    setResponseStatus(event, 503)
    return { error: 'service_unavailable', message: 'Retry once', retryAfter }
  }
  clearRetryAttempt(key)
  return { status: 'ok', attempts: n }
})
