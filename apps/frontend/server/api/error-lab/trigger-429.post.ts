/**
 * Error Lab — 429 Too Many Requests trigger (mock).
 * Simulates rate limiting with Retry-After header. No backend call — Nuxt-side mock.
 * Requirements: 6.1, Error Lab MVP
 */
export default defineEventHandler((event) => {
  const correlationId = crypto.randomUUID()
  setResponseStatus(event, 429)
  setHeader(event, 'Retry-After', 30)
  setHeader(event, 'Cache-Control', 'no-store')
  setHeader(event, 'X-Correlation-ID', correlationId)
  setHeader(event, 'Content-Type', 'application/problem+json')

  return {
    type: 'https://api.payment-quality.local/problems/rate-limit-exceeded',
    title: 'Too Many Requests',
    status: 429,
    detail: 'Rate limit exceeded. Retry after the indicated delay.',
    correlationId,
    error: 'rate_limit_exceeded',
    retryable: true,
    retryAfterSeconds: 30,
  }
})
