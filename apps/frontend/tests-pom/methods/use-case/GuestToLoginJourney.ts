/**
 * UC-W2-01 — guest hits /admin and must land on login.
 *
 * What changes: path (merchants, session-lab, users). Outcome is the same.
 * Fail-open: empty merchant table without login = fail.
 * Layer: E2E. Guest project must not destructure `api`.
 * Not End OIDC (UC-W2-18) and not menu Sign out (UC-W2-02).
 */

export const guestToLoginPaths = [
  { id: 'SCN-SES-01', path: '/admin/merchants' },
  { id: 'SCN-SES-02', path: '/admin/session-lab' },
] as const
