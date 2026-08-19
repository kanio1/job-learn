export default defineEventHandler((event) => {
  requireMirrorLab(event)
  const remainingMs = retryWindowRemainingMs(sessionKey(event))
  return { remainingMs, ttlMs: RETRY_TTL_MS }
})
