/**
 * EG — lab infrastructure, not domain money.
 *
 * Overlay: vite-plugin-checker steals clicks — fixtures locator handler; do not click it away as a “fix”.
 * IPv6: Node BffClient uses 127.0.0.1; browser OIDC uses localhost.
 * Sign out ≠ end_session. Confirm dismiss uses confirm-action-dismiss.
 * If-Match "stale-etag" is 400; stale version is "v99" → 412.
 */

export const errorGuessingNotes = [
  'EG-W2-01 overlay',
  'EG-W2-02 IPv6 ECONNREFUSED',
  'EG-W2-07 confirm-action-dismiss',
  'EG-W2-09 v99 vs stale-etag',
  'EG-W2-11 two logouts',
] as const

/** Node BFF REST must not resolve `localhost` to ::1. */
export function isIpv4LoopbackUrl(baseURL: string): boolean {
  try {
    return new URL(baseURL).hostname === '127.0.0.1'
  }
  catch {
    return false
  }
}
